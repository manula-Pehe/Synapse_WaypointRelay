package com.synapse.waypoint.planning.engine;

/** The headline numbers of a plan (docs/api.md §6). */
public record PlanSummary(int served, int deferred, int unavoidable, int chosen, int violations,
        int fridgeVehiclesUsed, int fridgeVehiclesAvailable) {
}
