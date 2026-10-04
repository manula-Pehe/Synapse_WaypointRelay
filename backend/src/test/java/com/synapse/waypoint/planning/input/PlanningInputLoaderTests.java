package com.synapse.waypoint.planning.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderFilters;
import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.core.reference.dto.OutletDto;
import com.synapse.waypoint.core.reference.dto.TravelDto;
import com.synapse.waypoint.core.reference.dto.VehicleDto;
import com.synapse.waypoint.core.reference.entity.AvailabilityStatus;
import com.synapse.waypoint.core.reference.service.ReferenceService;
import com.synapse.waypoint.planning.domain.Brand;
import com.synapse.waypoint.planning.domain.DockType;
import com.synapse.waypoint.planning.domain.Temperature;
import com.synapse.waypoint.planning.domain.VehicleTemperature;
import com.synapse.waypoint.planning.engine.input.InvalidPlanningInputException;
import com.synapse.waypoint.planning.engine.input.PlanningInput;
import com.synapse.waypoint.planning.engine.input.ServiceKey;

class PlanningInputLoaderTests {

    private static final LocalDate RUN_DATE = LocalDate.of(2030, 1, 10); // ISO week 2 of 2030
    private static final String DEPOT = "Depot-X";

    private final OrderService orderService = mock(OrderService.class);
    private final ReferenceService referenceService = mock(ReferenceService.class);
    private final PlanningInputLoader loader = new PlanningInputLoader(orderService, referenceService);

    private static OrderDto order(String ref, String outletId) {
        return new OrderDto("id-" + ref, ref, outletId, "Outlet " + outletId, "Fresh", TemperatureRequirement.CHILLED, 10,
                new BigDecimal("120.50"), new BigDecimal("1.250"), RUN_DATE, OrderStatus.CONFIRMED, OrderSource.SEED,
                false, 3, true, null, true, null, null);
    }

    private static OutletDto outlet(String id, String district) {
        return new OutletDto(id, id + " · " + district, "Fresh", district, DEPOT, "rear_dock", "van_only",
                LocalTime.of(4, 0), LocalTime.of(8, 0), null, null);
    }

    private static VehicleDto vehicle(String id) {
        return new VehicleDto(id, "van", "reefer", new BigDecimal("900"), new BigDecimal("6.5"), "diesel",
                new BigDecimal("8"), new BigDecimal("400"), DEPOT, AvailabilityStatus.AVAILABLE, null);
    }

    private void givenADepotDay() {
        when(orderService.findByRun(eq(RUN_DATE), eq(DEPOT), any())).thenReturn(List.of(order("A", "O1"), order("B", "O2")));
        when(referenceService.outlets(DEPOT)).thenReturn(List.of(outlet("O1", "North"), outlet("O2", "North"),
                outlet("O3", "Unserved")));
        when(referenceService.availableVehicles(RUN_DATE, DEPOT)).thenReturn(List.of(vehicle("V1")));
        when(referenceService.fuelUsed("V1", 2030, 2)).thenReturn(new BigDecimal("37.5"));
        when(referenceService.travel("North", DEPOT)).thenReturn(new TravelDto("North", DEPOT, "highway",
                new BigDecimal("40"), new BigDecimal("20"), 40, new BigDecimal("2"), 5));
        when(referenceService.serviceMinutes("Fresh", "rear_dock")).thenReturn(12);
    }

    @Test
    void shouldAskOnlyForConfirmedOrdersOfTheRunAndDepot() {
        givenADepotDay();

        loader.load(RUN_DATE, DEPOT);

        ArgumentCaptor<OrderFilters> filters = ArgumentCaptor.forClass(OrderFilters.class);
        verify(orderService).findByRun(eq(RUN_DATE), eq(DEPOT), filters.capture());
        assertThat(filters.getValue().status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void shouldMapOrdersOutletsVehiclesTravelAndServiceTimes() {
        givenADepotDay();

        PlanningInput input = loader.load(RUN_DATE, DEPOT);

        assertThat(input.runDate()).isEqualTo(RUN_DATE);
        assertThat(input.depot()).isEqualTo(DEPOT);
        assertThat(input.orders()).hasSize(2);
        assertThat(input.orders().get(0).brand()).isEqualTo(Brand.FRESH);
        assertThat(input.orders().get(0).temperature()).isEqualTo(Temperature.CHILLED);
        assertThat(input.orders().get(0).deferredYesterday()).isTrue();
        assertThat(input.orders().get(0).daysSinceLastServed()).isEqualTo(3);
        assertThat(input.outletsById().get("O1").isVanOnly()).isTrue();
        assertThat(input.outletsById().get("O1").dockType()).isEqualTo(DockType.REAR_DOCK);
        assertThat(input.vehicles()).singleElement().satisfies(vehicle -> {
            assertThat(vehicle.temperature()).isEqualTo(VehicleTemperature.REEFER);
            assertThat(vehicle.fuelUsedThisWeekLitres()).isEqualByComparingTo("37.5");
        });
        assertThat(input.travel("North").outboundMinutes()).isEqualTo(40);
        assertThat(input.serviceMinutes()).containsEntry(new ServiceKey(Brand.FRESH, DockType.REAR_DOCK), 12);
    }

    @Test
    void shouldLookUpFuelForTheIsoWeekOfTheRunDateNotTheCurrentDate() {
        givenADepotDay();

        loader.load(RUN_DATE, DEPOT);

        verify(referenceService).fuelUsed("V1", 2030, 2);
    }

    @Test
    void shouldFetchTravelAndServiceTimesOnlyForWhatTheOrdersNeed() {
        givenADepotDay();

        loader.load(RUN_DATE, DEPOT);

        verify(referenceService).travel("North", DEPOT);
        verify(referenceService, never()).travel(eq("Unserved"), anyString());
        verify(referenceService).serviceMinutes("Fresh", "rear_dock");
    }

    @Test
    void shouldOnlyReadOrdersWithOneQuery() {
        givenADepotDay();

        loader.load(RUN_DATE, DEPOT);

        verify(orderService).findByRun(any(), any(), any());
        verifyNoMoreInteractions(orderService);
    }

    @Test
    void shouldFailClearlyWhenAnOrderBelongsToAnOutletOutsideTheDepot() {
        givenADepotDay();
        when(orderService.findByRun(eq(RUN_DATE), eq(DEPOT), any())).thenReturn(List.of(order("A", "ELSEWHERE")));

        assertThatThrownBy(() -> loader.load(RUN_DATE, DEPOT))
                .isInstanceOf(InvalidPlanningInputException.class)
                .hasMessageContaining("ELSEWHERE");
    }

    @Test
    void shouldReturnAnEmptyInputWhenNothingIsConfirmed() {
        when(orderService.findByRun(eq(RUN_DATE), eq(DEPOT), any())).thenReturn(List.of());
        when(referenceService.outlets(DEPOT)).thenReturn(List.of());
        when(referenceService.availableVehicles(RUN_DATE, DEPOT)).thenReturn(List.of());

        PlanningInput input = loader.load(RUN_DATE, DEPOT);

        assertThat(input.orders()).isEmpty();
        assertThat(input.vehicles()).isEmpty();
    }
}
