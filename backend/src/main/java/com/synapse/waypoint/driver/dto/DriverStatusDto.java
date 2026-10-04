package com.synapse.waypoint.driver.dto;

import java.time.Instant;

/**
 * How a vehicle's run is going, from the driver's own records,
 * DeliveryQueryService.driverStatus).
 *
 * <p>lastSyncAt is the honest bit: a driver with no signal is not the same as a driver who
 * stopped working, and the board has to be able to tell them apart rather than showing a stale
 * position as if it were live.
 */
public record DriverStatusDto(
        String vehicleId,
        String driverId,
        String tripId,
        int stopsDone,
        Instant lastSyncAt,
        int openProblems,
        boolean offline) {
}