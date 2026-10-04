package com.synapse.waypoint.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderFilters;
import com.synapse.waypoint.core.order.entity.OrderEvent;
import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.repository.OrderEventRepository;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.reference.service.DispatcherDepotScope;
import com.synapse.waypoint.planning.domain.PlanStatus;
import com.synapse.waypoint.planning.domain.TripWindowType;
import com.synapse.waypoint.planning.dto.PlanDto;
import com.synapse.waypoint.planning.dto.PlanStopDto;
import com.synapse.waypoint.planning.dto.PlanSummaryDto;
import com.synapse.waypoint.planning.dto.PlanTripDto;
import com.synapse.waypoint.planning.dto.PlanVehicleDto;
import com.synapse.waypoint.planning.repository.DeferralRepository;
import com.synapse.waypoint.planning.service.PlanQueryService;

class DispatchReportServiceTest {

    @Test
    void reportsLateOutcomesByDistrictAndRecordedExceptions() {
        LocalDate day = LocalDate.of(2026, 10, 1);
        OffsetDateTime windowEnd = OffsetDateTime.parse("2026-10-01T07:00:00+05:30");
        DispatcherDepotScope scope = mock(DispatcherDepotScope.class);
        PlanQueryService plans = mock(PlanQueryService.class);
        OrderService orders = mock(OrderService.class);
        OrderEventRepository events = mock(OrderEventRepository.class);
        DeferralRepository deferrals = mock(DeferralRepository.class);
        DispatchLiveService live = mock(DispatchLiveService.class);
        when(scope.resolve("Peliyagoda")).thenReturn("Peliyagoda");
        when(live.read(day, "Peliyagoda")).thenReturn(new LiveBoardDto(day, "Peliyagoda", windowEnd,
                33, 3, 1, 1, null, null, List.of(), List.of()));

        PlanStopDto first = new PlanStopDto("s1", "o1", "A", "OUT001", 1, 1, 1,
                "AMBIENT", windowEnd.minusMinutes(30), windowEnd, BigDecimal.ZERO);
        PlanStopDto second = new PlanStopDto("s2", "o2", "B", "OUT002", 2, 2, 1,
                "AMBIENT", windowEnd.minusMinutes(30), windowEnd, BigDecimal.ZERO);
        PlanStopDto third = new PlanStopDto("s3", "o3", "C", "OUT003", 3, 3, 1,
                "AMBIENT", windowEnd.minusMinutes(30), windowEnd, BigDecimal.ZERO);
        PlanTripDto trip = new PlanTripDto("t1", 1, "Brand", "Colombo", TripWindowType.DAYTIME,
                windowEnd.minusHours(1), 90, BigDecimal.ONE, BigDecimal.ONE, List.of(first, second, third));
        PlanVehicleDto vehicle = new PlanVehicleDto("VEH001", "truck", "dry", BigDecimal.ONE,
                BigDecimal.ONE, 0, 270, 90, 480, List.of(trip));
        when(plans.publishedPlan(day, "Peliyagoda")).thenReturn(Optional.of(new PlanDto("p1", day,
                "Peliyagoda", 1, PlanStatus.PUBLISHED, new PlanSummaryDto(3, 1, 0, 1, 0, 0, 0, 0),
                List.of(vehicle))));
        List<OrderDto> runOrders = List.of(order("o1", OrderStatus.DELIVERED, day, windowEnd),
                order("o2", OrderStatus.PARTIAL, day, windowEnd),
                order("o3", OrderStatus.FAILED, day, windowEnd));
        when(orders.findByRun(eq(day), eq("Peliyagoda"), eq(OrderFilters.none()))).thenReturn(runOrders);
        when(orders.findByIds(anyCollection())).thenReturn(runOrders);
        when(events.findByOrderIdIn(anyCollection())).thenReturn(List.of(
                new OrderEvent("o1", windowEnd.plusMinutes(5).toInstant(), null, "DELIVERED",
                        OrderStatus.ON_THE_WAY, OrderStatus.DELIVERED, Map.of()),
                new OrderEvent("o2", windowEnd.minusMinutes(5).toInstant(), null, "PARTIAL",
                        OrderStatus.ON_THE_WAY, OrderStatus.PARTIAL, Map.of()),
                new OrderEvent("o3", windowEnd.plusMinutes(10).toInstant(), null, "FAILED",
                        OrderStatus.ON_THE_WAY, OrderStatus.FAILED, Map.of())));
        when(deferrals.findByPlanId("p1")).thenReturn(List.of());

        RunReportDto report = new DispatchReportService(scope, plans, orders, events, deferrals, live)
                .read(day, "Peliyagoda");

        assertThat(report.deferred()).isEqualTo(1);
        assertThat(report.failed()).isEqualTo(1);
        assertThat(report.partial()).isEqualTo(1);
        assertThat(report.lateByDistrict()).containsExactly(new RunReportDto.DistrictLate("Colombo", 1, 2));
        assertThat(report.exceptions()).extracting(RunReportDto.ExceptionItem::type)
                .containsExactly("FAILED", "PARTIAL");
    }

    private static OrderDto order(String id, OrderStatus status, LocalDate day, OffsetDateTime at) {
        return new OrderDto(id, id, "OUT001", "Outlet " + id, "Brand",
                TemperatureRequirement.AMBIENT, 1, BigDecimal.ONE, BigDecimal.ONE, day, status,
                OrderSource.STORE, false, 0, false, null, true, null, at);
    }
}
