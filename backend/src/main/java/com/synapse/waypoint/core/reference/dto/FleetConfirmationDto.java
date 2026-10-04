package com.synapse.waypoint.core.reference.dto;

import java.time.OffsetDateTime;

/** When the fleet was confirmed and by whom ({@code confirmedBy} is null when no one is recorded). */
public record FleetConfirmationDto(OffsetDateTime confirmedAt, String confirmedBy) {
}
