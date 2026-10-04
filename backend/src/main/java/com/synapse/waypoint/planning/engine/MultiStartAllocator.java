package com.synapse.waypoint.planning.engine;

import java.util.Comparator;

import com.synapse.waypoint.planning.engine.input.PlanningInput;

/**
 * Runs the greedy {@link Allocator} several times with different tie-breaking and new-trip vehicle
 * choices and keeps the best result. Run 0 is the plain run, so the answer is never worse than it. The
 * random source of each run is seeded from the run date, depot and run index: the same input always
 * gives the same plan.
 */
public class MultiStartAllocator {

    static final int RUN_COUNT = 300;

    private final PlanningInput input;
    private final PriorityScorer scorer;
    private final Allocator allocator;
    private final int runCount;
    private final Comparator<AllocationResult> betterFirst;

    public MultiStartAllocator(PlanningInput input, PriorityScorer scorer, Allocator allocator) {
        this(input, scorer, allocator, RUN_COUNT);
    }

    MultiStartAllocator(PlanningInput input, PriorityScorer scorer, Allocator allocator, int runCount) {
        this.input = input;
        this.scorer = scorer;
        this.allocator = allocator;
        this.runCount = runCount;
        this.betterFirst = Comparator
                .comparingInt(this::servedPriority).reversed()
                .thenComparing(Comparator.comparingInt(this::servedCount).reversed())
                .thenComparingInt(this::tripCount);
    }

    public AllocationResult allocate() {
        AllocationResult best = allocator.allocate(PlacementStrategy.plain());
        for (int runIndex = 1; runIndex < runCount; runIndex++) {
            AllocationResult candidate = allocator.allocate(strategyFor(runIndex));
            if (betterFirst.compare(candidate, best) < 0) {
                best = candidate;
            }
        }
        return best;
    }

    private PlacementStrategy strategyFor(int runIndex) {
        String seedText = input.runDate() + "|" + input.depot() + "|" + runIndex;
        NewTripVehicleChoice choice = runIndex % 2 == 1
                ? NewTripVehicleChoice.LARGEST_THAT_FITS
                : NewTripVehicleChoice.SMALLEST_THAT_FITS;
        return PlacementStrategy.randomised(seedText.hashCode(), choice);
    }

    private int servedPriority(AllocationResult result) {
        return result.days().stream()
                .flatMap(day -> day.trips().stream())
                .flatMap(trip -> trip.stops().stream())
                .mapToInt(stop -> scorer.score(stop.order()))
                .sum();
    }

    private int servedCount(AllocationResult result) {
        return result.days().stream().flatMap(day -> day.trips().stream()).mapToInt(trip -> trip.stops().size()).sum();
    }

    private int tripCount(AllocationResult result) {
        return result.days().stream().mapToInt(day -> day.trips().size()).sum();
    }
}
