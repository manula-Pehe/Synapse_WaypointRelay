package com.synapse.waypoint.planning.engine;

import java.util.Optional;
import java.util.Random;

/**
 * How one greedy run varies: an optional random source that shuffles orders of equal constraint and
 * priority, and the vehicle a new trip tries first. {@link #plain()} is fully deterministic without randomness.
 */
public record PlacementStrategy(Optional<Random> tieBreaker, NewTripVehicleChoice vehicleChoice) {

    public static PlacementStrategy plain() {
        return new PlacementStrategy(Optional.empty(), NewTripVehicleChoice.SMALLEST_THAT_FITS);
    }

    public static PlacementStrategy randomised(long seed, NewTripVehicleChoice vehicleChoice) {
        return new PlacementStrategy(Optional.of(new Random(seed)), vehicleChoice);
    }
}
