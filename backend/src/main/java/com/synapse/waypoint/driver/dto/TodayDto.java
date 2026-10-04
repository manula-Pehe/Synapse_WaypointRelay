package com.synapse.waypoint.driver.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the driver app needs to run a day, in one response (docs/api.md §7, F3).
 *
 * It is a single call on purpose: the phone caches the whole thing at sign-in so every screen after
 * that reads from disk and works with no signal (US-1.1). Splitting it would mean the driver waiting
 * on four requests in a depot car park.
 */
public record TodayDto(
        LocalDate runDate,
        String vehicleId,
        String vehicleType,
        /** The loader's count for the whole truck; the driver compares it against the stops (R0). */
        Integer loadedCases,
        /** Null until the driver accepts the load, which is what R0 is waiting on. */
        Boolean loadAccepted,
        List<DriverTripDto> trips) {

    public TodayDto {
        trips = List.copyOf(trips);
    }
}