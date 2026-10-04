package com.synapse.waypoint.core.settings.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotNull;

/** {@code POST /api/settings/clock} - move the demo clock to {@code at}. */
public record MoveClockRequest(@NotNull OffsetDateTime at) {
}
