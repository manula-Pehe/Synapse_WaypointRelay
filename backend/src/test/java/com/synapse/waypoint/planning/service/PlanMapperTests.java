package com.synapse.waypoint.planning.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.reference.dto.VehicleDto;
import com.synapse.waypoint.core.reference.entity.AvailabilityStatus;
import com.synapse.waypoint.planning.domain.PlanStatus;
import com.synapse.waypoint.planning.domain.TripWindowType;
import com.synapse.waypoint.planning.dto.PlanDto;
import com.synapse.waypoint.planning.dto.PlanSummaryDto;
import com.synapse.waypoint.planning.entity.Plan;
import com.synapse.waypoint.planning.entity.Stop;
import com.synapse.waypoint.planning.entity.Trip;

/** Maps stored plans from preloaded orders and vehicles. Data is invented. */
class PlanMapperTests {

    private static final LocalDate RUN_DATE = LocalDate.parse("2030-01-10");
    private static final Instant DEPARTURE = Instant.parse("2030-01-09T22:00:00Z");

    private final PlanMapper mapper = new PlanMapper();

    @Test
    void shouldGroupTripsUnderVehiclesWithMinutesUsedAgainstEachBudget() {
        Plan plan = plan();
        List<Trip> trips = List.of(
                trip("trp-b", "VEH-B", 1, TripWindowType.FRESH, 100),
                trip("trp-a2", "VEH-A", 2, TripWindowType.DAYTIME, 150),
                trip("trp-a1", "VEH-A", 1, TripWindowType.FRESH, 60));
        Map<String, List<Stop>> stops = Map.of("trp-a1", List.of(stop("stp-2", "trp-a1", "ord-2", 2),
                stop("stp-1", "trp-a1", "ord-1", 1)));

        PlanDto dto = mapper.toDto(plan, trips, stops,
                Map.of("ord-1", order("ord-1", "R-1", 12), "ord-2", order("ord-2", "R-2", 7)),
                Map.of("VEH-A", vehicle("VEH-A"), "VEH-B", vehicle("VEH-B")));

        assertThat(dto.vehicles()).extracting("vehicleId").containsExactly("VEH-A", "VEH-B");
        assertThat(dto.vehicles().get(0)).extracting("freshMinutesUsed", "freshBudget", "daytimeMinutesUsed",
                "daytimeBudget").containsExactly(60, 270, 150, 480);
        assertThat(dto.vehicles().get(0).trips()).extracting("tripNo").containsExactly(1, 2);
        assertThat(dto.vehicles().get(0).trips().get(0).stops()).extracting("orderRef", "seq", "units")
                .containsExactly(tuple("R-1", 1, 12),
                        tuple("R-2", 2, 7));
        assertThat(dto.summary().served()).isEqualTo(2);
    }

    private static Plan plan() {
        PlanSummaryDto summary = new PlanSummaryDto(2, 0, 0, 0, 0, 1, 1, 0);
        return new Plan("pln-1", RUN_DATE, "Testdepot", 1, PlanStatus.DRAFT, summary.toStored(), null, null,
                DEPARTURE, null);
    }

    private static Trip trip(String id, String vehicleId, int tripNo, TripWindowType window, int minutes) {
        return new Trip(id, "pln-1", vehicleId, tripNo, "Fresh", "Northvale", window, DEPARTURE, minutes,
                new BigDecimal("100.00"), new BigDecimal("1.000"), new BigDecimal("40.00"));
    }

    private static Stop stop(String id, String tripId, String orderId, int seq) {
        return new Stop(id, tripId, orderId, seq, 3 - seq, DEPARTURE, DEPARTURE.plusSeconds(1800),
                new BigDecimal("0.100"));
    }

    private static OrderDto order(String id, String ref, int units) {
        return new OrderDto(id, ref, "OUT-1", "Outlet", "Fresh", TemperatureRequirement.CHILLED, units,
                BigDecimal.TEN, BigDecimal.ONE, RUN_DATE, OrderStatus.CONFIRMED, OrderSource.SEED, false, 1, false,
                null, true, null, null);
    }

    private static VehicleDto vehicle(String id) {
        return new VehicleDto(id, "van", "reefer", new BigDecimal("1000"), new BigDecimal("6"), "diesel",
                new BigDecimal("8"), new BigDecimal("300"), "Testdepot", AvailabilityStatus.AVAILABLE, null);
    }
}
