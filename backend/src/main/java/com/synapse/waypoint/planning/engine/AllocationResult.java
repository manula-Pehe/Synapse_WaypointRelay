package com.synapse.waypoint.planning.engine;

import java.util.List;

/** What the allocator produced: the trips of each used vehicle and the orders left over. */
public record AllocationResult(List<VehicleDay> days, List<UnplacedOrder> unplaced) {

    public AllocationResult {
        days = List.copyOf(days);
        unplaced = List.copyOf(unplaced);
    }
}
