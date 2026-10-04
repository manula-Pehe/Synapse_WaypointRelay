package com.synapse.waypoint.planning.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.synapse.waypoint.planning.dto.DeferralDto;
import com.synapse.waypoint.planning.dto.PlanDto;
import com.synapse.waypoint.planning.dto.PlanTripDto;
import com.synapse.waypoint.planning.dto.StopPlacementDto;

/**
 * Read-only view of the PUBLISHED plan for other modules (docs/api.md §11). Drafts and superseded
 * plans are never returned. Callers may be any role, so nothing here is limited to the signed-in user.
 */
public interface PlanQueryService {

    /** The vehicle's trips on the run date, in trip order; empty when no published plan uses it. */
    List<PlanTripDto> tripsForVehicle(LocalDate runDate, String vehicleId);

    /** Every trip of the depot's published plan, by vehicle and trip number; empty when none is published. */
    List<PlanTripDto> tripsForDepot(LocalDate runDate, String depot);

    /** Where the published plan delivers the order, if it does. */
    Optional<StopPlacementDto> stopForOrder(String orderId);

    /** Why the published plan left the order out (the latest such deferral), if it did. */
    Optional<DeferralDto> deferralForOrder(String orderId);

    Optional<PlanDto> publishedPlan(LocalDate runDate, String depot);
}
