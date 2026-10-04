package com.synapse.waypoint.planning.engine;

import static com.synapse.waypoint.planning.engine.PlanningTestData.aVehicle;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anInput;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anOrder;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anOutlet;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.synapse.waypoint.planning.engine.input.PlanningInput;

class PlanningEngineTests {

    private final PlanningEngine engine = new PlanningEngine();

    private static PlanningInput smallDay() {
        return anInput()
                .outlet(anOutlet().id("O1").window("04:00", "08:00"))
                .outlet(anOutlet().id("O2").window("04:00", "07:00"))
                .outlet(anOutlet().id("O3").window("04:00", "06:00"))
                .vehicle(aVehicle().id("DRY").capacity("1000", "10"))
                .vehicle(aVehicle().id("COLD").reefer().capacity("1000", "10"))
                .order(anOrder().ref("A").outlet("O1"))
                .order(anOrder().ref("B").outlet("O2"))
                .order(anOrder().ref("C").outlet("O3").chilled())
                .order(anOrder().ref("HUGE").outlet("O1").weightKg("9000"))
                .build();
    }

    @Test
    void shouldSummariseServedAndDeferredOrders() {
        PlanSummary summary = engine.plan(smallDay()).summary();

        assertThat(summary).isEqualTo(new PlanSummary(3, 1, 1, 0, 0, 1, 1));
    }

    @Test
    void shouldVerifyTheWholePlanWithNoViolations() {
        PlanningResult result = engine.plan(smallDay());

        assertThat(result.violations()).isEmpty();
        assertThat(result.summary().violations()).isZero();
    }

    @Test
    void shouldLoadStopsInReverseOfDeliveryOrder() {
        PlannedTrip dryTrip = engine.plan(smallDay()).trips().stream()
                .filter(trip -> trip.vehicleId().equals("DRY")).findFirst().orElseThrow();

        assertThat(dryTrip.stops()).extracting(stop -> stop.order().ref()).containsExactly("B", "A");
        assertThat(dryTrip.stops()).extracting(PlannedStop::seq).containsExactly(1, 2);
        assertThat(dryTrip.stops()).extracting(PlannedStop::loadSeq).containsExactly(2, 1);
    }

    @Test
    void shouldRecordTripTotalsDepartureAndArrivalWindows() {
        PlannedTrip dryTrip = engine.plan(smallDay()).trips().stream()
                .filter(trip -> trip.vehicleId().equals("DRY")).findFirst().orElseThrow();

        assertThat(dryTrip.tripNo()).isEqualTo(1);
        assertThat(dryTrip.departAt()).isEqualTo(LocalTime.of(3, 30));
        assertThat(dryTrip.minutes()).isEqualTo(40 + 5 + 2 * 10);
        assertThat(dryTrip.weightKg()).isEqualByComparingTo("200");
        assertThat(dryTrip.km()).isEqualByComparingTo("42");
        assertThat(dryTrip.stops().get(0).arriveFrom()).isEqualTo(LocalTime.of(4, 0));
        assertThat(dryTrip.stops().get(0).arriveTo()).isEqualTo(LocalTime.of(4, 25));
    }

    @Test
    void shouldReadNoClockSoTheSameInputGivesTheSamePlan() {
        assertThat(engine.plan(smallDay())).isEqualTo(engine.plan(smallDay()));
    }

    @Test
    void shouldStillServeAStopThatArrivesLateAndWarnAboutIt() {
        PlanningInput input = anInput()
                .outlet(anOutlet().id("O1").window("04:00", "04:05"))
                .vehicle(aVehicle())
                .order(anOrder().ref("LATE").outlet("O1"))
                .build();

        PlanningResult result = engine.plan(input);

        assertThat(result.summary().served()).isEqualTo(1);
        assertThat(result.violations()).isEmpty();
        assertThat(result.warnings()).hasSize(1);
        assertThat(result.trips().get(0).stops().get(0).lateRisk()).isEqualByComparingTo("1");
    }
}
