package com.synapse.waypoint.driver.service;

import com.synapse.waypoint.driver.dto.TodayDto;

/**
 * What the driver's phone needs for the run it is on (docs/api.md §7, F3).
 *
 * The plan is read through the planning module's {@code PlanQueryService} rather than from the planning tables,
 * so a driver never sees a draft or a superseded plan (R2c depends on this: the published plan is the
 * one the driver follows).
 */
public interface TodayService {

    /**
     * Today's run for the signed-in driver's own vehicle.
     *
     * @throws com.synapse.waypoint.common.error.DomainException {@code FORBIDDEN} when the signed-in
     *         user has no vehicle, so a dispatcher or store account cannot read a driver's run
     */
    TodayDto today();

    /**
     * Marks the load accepted and moves the trip's orders on the way (R0).
     *
     * @throws com.synapse.waypoint.common.error.DomainException {@code VALIDATION} when the trip does
     *         not belong to the driver's vehicle, {@code NOT_FOUND} when there is no such trip
     */
    TodayDto acceptTrip(String tripId);
}