package com.synapse.waypoint.planning.engine;

import java.util.ArrayList;
import java.util.List;

import com.synapse.waypoint.planning.engine.input.VehicleInput;

/** One vehicle with all the trips proposed for it on the run date, in trip order. */
public record VehicleDay(VehicleInput vehicle, List<TripDraft> trips) {

    public VehicleDay {
        trips = List.copyOf(trips);
    }

    public static VehicleDay idle(VehicleInput vehicle) {
        return new VehicleDay(vehicle, List.of());
    }

    public VehicleDay withTrip(TripDraft trip) {
        List<TripDraft> extended = new ArrayList<>(trips);
        extended.add(trip);
        return new VehicleDay(vehicle, extended);
    }

    public VehicleDay withTripReplaced(int tripIndex, TripDraft trip) {
        List<TripDraft> replaced = new ArrayList<>(trips);
        replaced.set(tripIndex, trip);
        return new VehicleDay(vehicle, replaced);
    }
}
