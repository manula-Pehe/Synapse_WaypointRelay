package com.synapse.waypoint.core.reference.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.synapse.waypoint.core.reference.entity.AvailabilityStatus;

/** {@code PUT /api/dispatch/fleet/{vehicleId}} (D2v). The reason is required unless the status is AVAILABLE. */
public record UpdateAvailabilityRequest(@NotNull LocalDate runDate, @NotNull AvailabilityStatus status,
        @Size(max = 200) String reason) {
}
