package com.synapse.waypoint.planning.engine;

import static com.synapse.waypoint.planning.engine.PlanningTestData.aVehicle;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anInput;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anOrder;
import static com.synapse.waypoint.planning.engine.PlanningTestData.anOutlet;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.synapse.waypoint.planning.engine.PlanningTestData.InputBuilder;
import com.synapse.waypoint.planning.engine.input.PlanningInput;
import com.synapse.waypoint.planning.engine.input.TravelInput;
import com.synapse.waypoint.planning.engine.rule.RuleChecker;

class MultiStartAllocatorTests {

    private static final String SMALL_VAN_WEIGHT = "200";
    private static final String SMALL_VAN_VOLUME = "2";

    /** A small van that fills up with two ordinary trips, a truck, and one outlet only the van can reach. */
    private static InputBuilder vanContestFixture() {
        return anInput()
                .travel(new TravelInput("South", new BigDecimal("10"), 30, new BigDecimal("1"), 5))
                .travel(new TravelInput("East", new BigDecimal("10"), 30, new BigDecimal("1"), 5))
                .outlet(anOutlet().id("N1"))
                .outlet(anOutlet().id("S1").district("South"))
                .outlet(anOutlet().id("E1").district("East").vanOnly())
                .vehicle(aVehicle().id("VAN").van().capacity(SMALL_VAN_WEIGHT, SMALL_VAN_VOLUME))
                .vehicle(aVehicle().id("TRUCK"))
                .order(anOrder().ref("ORD-N").outlet("N1").daysSinceLastServed(3))
                .order(anOrder().ref("ORD-S").outlet("S1").daysSinceLastServed(3))
                .order(anOrder().ref("ORD-E").outlet("E1"));
    }

    private static Allocator allocatorFor(PlanningInput input) {
        PriorityScorer scorer = new PriorityScorer();
        return new Allocator(input, scorer, RuleChecker.standard(new TripCalculator(input)));
    }

    private static MultiStartAllocator multiStartFor(PlanningInput input) {
        return new MultiStartAllocator(input, new PriorityScorer(), allocatorFor(input));
    }

    private static List<String> placementOf(AllocationResult result) {
        return result.days().stream()
                .flatMap(day -> day.trips().stream().map(trip -> day.vehicle().id() + ":" + trip.stops().stream()
                        .map(stop -> stop.order().ref()).toList()))
                .toList();
    }

    private static int servedBy(AllocationResult result) {
        return result.days().stream().flatMap(day -> day.trips().stream()).mapToInt(trip -> trip.stops().size()).sum();
    }

    @Test
    void shouldServeTheVanOnlyOrderBecauseMostConstrainedOrdersGoFirst() {
        AllocationResult result = allocatorFor(vanContestFixture().build()).allocate();

        assertThat(result.unplaced()).isEmpty();
    }

    @Test
    void shouldGivePlanningTheSamePlanEveryTime() {
        PlanningInput input = vanContestFixture().build();

        AllocationResult first = multiStartFor(input).allocate();
        AllocationResult second = multiStartFor(input).allocate();

        assertThat(placementOf(second)).isEqualTo(placementOf(first));
        assertThat(second.unplaced()).isEqualTo(first.unplaced());
    }

    @Test
    void shouldNeverServeFewerOrdersThanThePlainRun() {
        PlanningInput input = vanContestFixture()
                .order(anOrder().ref("ORD-X").outlet("N1").weightKg("150"))
                .order(anOrder().ref("ORD-Y").outlet("S1").weightKg("900").volumeM3("8"))
                .build();

        AllocationResult plain = allocatorFor(input).allocate();
        AllocationResult best = multiStartFor(input).allocate();

        assertThat(servedBy(best)).isGreaterThanOrEqualTo(servedBy(plain));
    }
}
