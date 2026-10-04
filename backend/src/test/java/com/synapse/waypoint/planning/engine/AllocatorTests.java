package com.synapse.waypoint.planning.engine;

import static com.synapse.waypoint.planning.engine.PlanningTestData.aVehicle;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anInput;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anOrder;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anOutlet;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.synapse.waypoint.planning.domain.Brand;
import com.synapse.waypoint.planning.domain.RuleCode;
import com.synapse.waypoint.planning.engine.PlanningTestData.InputBuilder;
import com.synapse.waypoint.planning.engine.input.PlanningInput;
import com.synapse.waypoint.planning.engine.input.TravelInput;
import com.synapse.waypoint.planning.engine.rule.RuleChecker;
import com.synapse.waypoint.planning.engine.rule.RuleViolation;

class AllocatorTests {

    private static InputBuilder withOutlets() {
        return anInput()
                .outlet(anOutlet().id("O1").window("04:00", "08:00"))
                .outlet(anOutlet().id("O2").window("04:00", "07:00"))
                .outlet(anOutlet().id("O3").window("04:00", "06:00"))
                .outlet(anOutlet().id("S1").district("South"))
                .outlet(anOutlet().id("ST").window("08:00", "18:00"));
    }

    private static AllocationResult allocate(PlanningInput input) {
        return new Allocator(input, new PriorityScorer(), RuleChecker.standard(new TripCalculator(input))).allocate();
    }

    private static List<String> refsOf(TripDraft trip) {
        return trip.stops().stream().map(stop -> stop.order().ref()).toList();
    }

    @Test
    void shouldShareOneTripBetweenOrdersOfTheSameBrandAndDistrict() {
        AllocationResult result = allocate(withOutlets()
                .vehicle(aVehicle())
                .order(anOrder().ref("A").outlet("O1")).order(anOrder().ref("B").outlet("O2")).build());

        assertThat(result.days()).hasSize(1);
        assertThat(result.days().get(0).trips()).hasSize(1);
        assertThat(result.days().get(0).trips().get(0).stops()).hasSize(2);
        assertThat(result.unplaced()).isEmpty();
    }

    @Test
    void shouldOrderStopsByWindowClose() {
        AllocationResult result = allocate(withOutlets()
                .vehicle(aVehicle())
                .order(anOrder().ref("A").outlet("O1")).order(anOrder().ref("B").outlet("O3"))
                .order(anOrder().ref("C").outlet("O2")).build());

        assertThat(refsOf(result.days().get(0).trips().get(0))).containsExactly("B", "C", "A");
    }

    @Test
    void shouldGiveASecondTripToAnotherDistrictOnTheSameVehicle() {
        AllocationResult result = allocate(withOutlets()
                .travel(new TravelInput("South", new BigDecimal("10"), 30, new BigDecimal("1"), 5))
                .vehicle(aVehicle())
                .order(anOrder().ref("A").outlet("O1")).order(anOrder().ref("B").outlet("S1")).build());

        assertThat(result.days()).hasSize(1);
        assertThat(result.days().get(0).trips()).extracting(TripDraft::district).containsExactlyInAnyOrder("North", "South");
    }

    @Test
    void shouldNeverPutTwoBrandsOnOneTrip() {
        AllocationResult result = allocate(withOutlets()
                .vehicle(aVehicle())
                .order(anOrder().ref("A").outlet("O1"))
                .order(anOrder().ref("B").outlet("ST").brand(Brand.STYLE)).build());

        assertThat(result.days().get(0).trips()).extracting(TripDraft::brand)
                .containsExactlyInAnyOrder(Brand.FRESH, Brand.STYLE);
    }

    @Test
    void shouldChooseTheSmallestVehicleThatFits() {
        AllocationResult result = allocate(withOutlets()
                .vehicle(aVehicle().id("BIG").capacity("5000", "50"))
                .vehicle(aVehicle().id("SMALL").capacity("500", "5"))
                .order(anOrder().ref("A").outlet("O1")).build());

        assertThat(result.days()).extracting(day -> day.vehicle().id()).containsExactly("SMALL");
    }

    @Test
    void shouldKeepFridgeVehiclesFreeForChilledOrders() {
        AllocationResult result = allocate(withOutlets()
                .vehicle(aVehicle().id("FRIDGE").reefer().capacity("100", "1"))
                .vehicle(aVehicle().id("DRY").capacity("5000", "50"))
                .order(anOrder().ref("A").outlet("O1")).build());

        assertThat(result.days()).extracting(day -> day.vehicle().id()).containsExactly("DRY");
    }

    @Test
    void shouldUseAFridgeVehicleForAmbientOrdersWhenNothingElseFits() {
        AllocationResult result = allocate(withOutlets()
                .vehicle(aVehicle().id("FRIDGE").reefer())
                .order(anOrder().ref("A").outlet("O1")).build());

        assertThat(result.days()).extracting(day -> day.vehicle().id()).containsExactly("FRIDGE");
    }

    @Test
    void shouldCarryChilledOrdersOnlyOnFridgeVehicles() {
        AllocationResult result = allocate(withOutlets()
                .vehicle(aVehicle().id("DRY"))
                .vehicle(aVehicle().id("FRIDGE").reefer().capacity("5000", "50"))
                .order(anOrder().ref("A").outlet("O1").chilled()).build());

        assertThat(result.days()).extracting(day -> day.vehicle().id()).containsExactly("FRIDGE");
    }

    @Test
    void shouldServeTheHigherPriorityOrderWhenOnlyOneFits() {
        // The 12 litre quota rules out a second trip, so only one 100 kg order can be carried.
        AllocationResult result = allocate(withOutlets()
                .vehicle(aVehicle().capacity("150", "10").weeklyQuotaLitres("12"))
                .order(anOrder().ref("LOW").outlet("O1").weightKg("100"))
                .order(anOrder().ref("HIGH").outlet("O2").weightKg("100").daysSinceLastServed(3)).build());

        assertThat(refsOf(result.days().get(0).trips().get(0))).containsExactly("HIGH");
        assertThat(result.unplaced()).extracting(unplaced -> unplaced.order().ref()).containsExactly("LOW");
    }

    @Test
    void shouldPlaceChilledBeforeAmbientAtEqualPriorityScoreAndLargerFirst() {
        AllocationResult result = allocate(withOutlets()
                .vehicle(aVehicle().reefer().capacity("150", "10"))
                .order(anOrder().ref("SMALL").outlet("O1").chilled().weightKg("60"))
                .order(anOrder().ref("LARGE").outlet("O2").chilled().weightKg("140")).build());

        assertThat(refsOf(result.days().get(0).trips().get(0))).containsExactly("LARGE");
    }

    @Test
    void shouldRecordTheRuleThatBlockedAnUnplacedOrder() {
        AllocationResult result = allocate(withOutlets()
                .vehicle(aVehicle().capacity("100", "10"))
                .order(anOrder().ref("HUGE").outlet("O1").weightKg("500")).build());

        assertThat(result.days()).isEmpty();
        assertThat(result.unplaced().get(0).lastBlock()).get().extracting(RuleViolation::rule)
                .isEqualTo(RuleCode.OVER_WEIGHT);
    }

    @Test
    void shouldGiveTheSameResultOnEveryRun() {
        PlanningInput input = withOutlets()
                .vehicle(aVehicle().id("V1").capacity("300", "5")).vehicle(aVehicle().id("V2").capacity("300", "5"))
                .order(anOrder().ref("A").outlet("O1").weightKg("200")).order(anOrder().ref("B").outlet("O2").weightKg("200"))
                .order(anOrder().ref("C").outlet("O3").weightKg("200")).build();

        assertThat(allocate(input)).isEqualTo(allocate(input));
    }
}
