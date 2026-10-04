package com.synapse.waypoint.planning.engine;

import static com.synapse.waypoint.planning.engine.PlanningTestData.aStop;
import static com.synapse.waypoint.planning.engine.PlanningTestData.aVehicle;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anInput;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anOrder;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anOutlet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.synapse.waypoint.planning.domain.Brand;
import com.synapse.waypoint.planning.domain.DockType;
import com.synapse.waypoint.planning.engine.input.InvalidPlanningInputException;
import com.synapse.waypoint.planning.engine.input.VehicleInput;

class TripCalculatorTests {

    private final TripCalculator calculator = new TripCalculator(anInput().build());
    private final VehicleInput vehicle = aVehicle().kmPerLitre("4").build();

    private StopCandidate stop(String ref, String outletId) {
        return aStop(anOrder().ref(ref), anOutlet().id(outletId));
    }

    private TripDraft threeStopTrip() {
        return new TripDraft(List.of(stop("A", "O1"), stop("B", "O2"), stop("C", "O3")));
    }

    @Test
    void shouldComputeMinutesAsOutboundPlusInterStopPlusService() {
        // 40 outbound + 5 × 2 between stops + 3 × 10 service
        assertThat(calculator.minutes(threeStopTrip())).isEqualTo(80);
    }

    @Test
    void shouldUseServiceMinutesOfEachStopsBrandAndDock() {
        TripCalculator custom = new TripCalculator(
                anInput().serviceMinutes(Brand.FRESH, DockType.STREET, 25).build());
        TripDraft trip = new TripDraft(List.of(aStop(anOrder(), anOutlet().dockType(DockType.STREET))));

        assertThat(custom.minutes(trip)).isEqualTo(40 + 25);
    }

    @Test
    void shouldComputeKmAsTwiceDepotLegPlusInterStopLegs() {
        // 2 × 20 + 2 × 2
        assertThat(calculator.km(threeStopTrip())).isEqualByComparingTo("44");
    }

    @Test
    void shouldComputeLitresAsKmOverKmPerLitre() {
        assertThat(calculator.litres(threeStopTrip(), vehicle)).isEqualByComparingTo("11");
    }

    @Test
    void shouldSumLitresOverAllTripsOfTheDay() {
        TripDraft single = new TripDraft(List.of(stop("A", "O1")));
        VehicleDay day = VehicleDay.idle(vehicle).withTrip(single).withTrip(single);

        assertThat(calculator.litres(day)).isEqualByComparingTo("20");
    }

    @Test
    void shouldDepartFreshTripsAtThreeThirty() {
        VehicleDay day = VehicleDay.idle(vehicle).withTrip(threeStopTrip());

        assertThat(calculator.timing(day, 0).departure()).isEqualTo(LocalTime.of(3, 30));
    }

    @Test
    void shouldDepartDaytimeTripsAtEight() {
        TripDraft styleTrip = new TripDraft(List.of(aStop(anOrder().brand(Brand.STYLE), anOutlet())));

        assertThat(calculator.timing(VehicleDay.idle(vehicle).withTrip(styleTrip), 0).departure())
                .isEqualTo(LocalTime.of(8, 0));
    }

    @Test
    void shouldDepartSecondTripWhenTheFirstOfTheSameWindowEnds() {
        TripDraft trip = threeStopTrip();
        VehicleDay day = VehicleDay.idle(vehicle).withTrip(trip).withTrip(trip);

        assertThat(calculator.timing(day, 1).departure()).isEqualTo(LocalTime.of(3, 30).plusMinutes(80));
    }

    @Test
    void shouldPredictArrivalAtEachStopFromServiceAndInterStopTime() {
        TripTiming timing = calculator.timing(VehicleDay.idle(vehicle).withTrip(threeStopTrip()), 0);

        // depart 3:30 + 40 outbound = 4:10; then +10 service +5 drive per stop
        assertThat(timing.arrivals()).containsExactly(LocalTime.of(4, 10), LocalTime.of(4, 25), LocalTime.of(4, 40));
        assertThat(timing.minutes()).isEqualTo(80);
    }

    @Test
    void shouldFailClearlyWhenTravelTimesAreMissing() {
        TripDraft elsewhere = new TripDraft(List.of(aStop(anOrder(), anOutlet().district("Nowhere"))));

        assertThatThrownBy(() -> calculator.minutes(elsewhere))
                .isInstanceOf(InvalidPlanningInputException.class)
                .hasMessageContaining("Nowhere");
    }
}
