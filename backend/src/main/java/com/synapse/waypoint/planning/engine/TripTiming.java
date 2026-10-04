package com.synapse.waypoint.planning.engine;

import java.time.LocalTime;
import java.util.List;

/** When a trip leaves, how long it takes and when it reaches each stop (same order as its stops). */
public record TripTiming(LocalTime departure, int minutes, List<LocalTime> arrivals) {

    public TripTiming {
        arrivals = List.copyOf(arrivals);
    }
}
