package com.synapse.waypoint.planning.dto;

import java.time.OffsetDateTime;

/** Where a published plan delivers an order: the stop plus the trip and vehicle it belongs to. */
public record StopPlacementDto(String tripId, String vehicleId, int tripNo, OffsetDateTime departAt,
        PlanStopDto stop) {
}
