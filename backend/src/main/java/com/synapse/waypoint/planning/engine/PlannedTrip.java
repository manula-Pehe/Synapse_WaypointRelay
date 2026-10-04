package com.synapse.waypoint.planning.engine;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import com.synapse.waypoint.planning.domain.Brand;
import com.synapse.waypoint.planning.domain.TripWindowType;

/** A trip as the plan records it. */
public record PlannedTrip(String vehicleId, int tripNo, Brand brand, String district, TripWindowType windowType,
        LocalTime departAt, int minutes, BigDecimal weightKg, BigDecimal volumeM3, BigDecimal km,
        List<PlannedStop> stops) {

    public PlannedTrip {
        stops = List.copyOf(stops);
    }
}
