package com.synapse.waypoint.dispatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.synapse.waypoint.common.time.DemoClock;
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

class DispatchLiveServiceTest {

    @Test
    void aggregatesRecordedOutcomesAndSortsCriticalItemsFirst() {
        LocalDate day = LocalDate.of(2026, 10, 1);
        OffsetDateTime at = OffsetDateTime.parse("2026-10-01T06:00:00+05:30");
        DispatcherDepotScope scope = mock(DispatcherDepotScope.class);
        PlanQueryService plans = mock(PlanQueryService.class);
        OrderService orders = mock(OrderService.class);
        OrderEventRepository events = mock(OrderEventRepository.class);
        DeferralRepository deferrals = mock(DeferralRepository.class);
        DemoClock clock = mock(DemoClock.class);
        when(scope.resolve("Peliyagoda")).thenReturn("Peliyagoda");
        when(clock.now()).thenReturn(at.toInstant());

        PlanStopDto delivered = stop("s1", "o1", "OUT001", at.plusMinutes(30), BigDecimal.ZERO);
        PlanStopDto failed = stop("s2", "o2", "OUT002", at.plusMinutes(30), BigDecimal.ZERO);
        PlanStopDto atRisk = stop("s3", "o3", "OUT003", at.plusMinutes(30), new BigDecimal("0.25"));
        PlanTripDto trip = new PlanTripDto("t1", 1, "Fresh", "Colombo", TripWindowType.FRESH,
                at, 60, BigDecimal.ONE, BigDecimal.ONE, List.of(delivered, failed, atRisk));
        PlanVehicleDto vehicle = new PlanVehicleDto("VEH001", "truck", "reefer", BigDecimal.ONE,
                BigDecimal.ONE, 60, 270, 0, 480, List.of(trip));
        PlanDto plan = new PlanDto("p1", day, "Peliyagoda", 1, PlanStatus.PUBLISHED,
                new PlanSummaryDto(3, 2, 1, 1, 0, 1, 2, 1), List.of(vehicle));
        when(plans.publishedPlan(day, "Peliyagoda")).thenReturn(Optional.of(plan));

        List<OrderDto> runOrders = List.of(order("o1", OrderStatus.DELIVERED, day, at),
                order("o2", OrderStatus.FAILED, day, at), order("o3", OrderStatus.ON_THE_WAY, day, at));
        when(orders.findByRun(eq(day), eq("Peliyagoda"), eq(OrderFilters.none()))).thenReturn(runOrders);
        when(orders.findByIds(anyCollection())).thenReturn(runOrders);
        when(events.findByOrderIdIn(anyCollection())).thenReturn(List.of(
                new OrderEvent("o1", at.plusMinutes(20).toInstant(), null, "DELIVERED",
                        OrderStatus.ON_THE_WAY, OrderStatus.DELIVERED, Map.of()),
                new OrderEvent("o2", at.plusMinutes(40).toInstant(), null, "FAILED",
                        OrderStatus.ON_THE_WAY, OrderStatus.FAILED, Map.of())));
        when(deferrals.findByPlanId("p1")).thenReturn(List.of());

        LiveBoardDto result = new DispatchLiveService(scope, plans, orders, events, deferrals, clock)
                .read(day, "Peliyagoda");

        assertThat(result.onTimePercent()).isEqualTo(50);
        assertThat(result.completedStops()).isEqualTo(2);
        assertThat(result.deferredToday()).isEqualTo(2);
        assertThat(result.fridgeTruckUsePercent()).isEqualTo(50);
        assertThat(result.needsAttention()).extracting(LiveBoardDto.Attention::type)
                .containsExactly("FAILED_DELIVERY", "LATE_RISK");
        assertThat(result.trips()).singleElement().satisfies(progress -> {
            assertThat(progress.stopsDone()).isEqualTo(2);
            assertThat(progress.status()).isEqualTo("ON_THE_WAY");
            assertThat(progress.lastSync()).isNull();
        });
    }

    private static PlanStopDto stop(String id, String orderId, String outletId,
            OffsetDateTime arriveTo, BigDecimal lateRisk) {
        return new PlanStopDto(id, orderId, orderId, outletId, 1, 1, 1, "CHILLED",
                arriveTo.minusMinutes(30), arriveTo, lateRisk);
    }

    private static OrderDto order(String id, OrderStatus status, LocalDate day, OffsetDateTime at) {
        return new OrderDto(id, id, id.replace('o', '0'), "Outlet " + id, "Fresh",
                TemperatureRequirement.CHILLED, 1, BigDecimal.ONE, BigDecimal.ONE, day, status,
                OrderSource.STORE, false, 0, false, null, true, null, at);
    }
}
