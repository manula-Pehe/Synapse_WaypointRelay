package com.synapse.waypoint.core.reference.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code POST /api/dispatch/fleet/confirm} - the dispatcher confirms the fleet for a run. */
public record ConfirmFleetRequest(@NotNull LocalDate runDate, @NotBlank @Size(max = 20) String depot) {
}
