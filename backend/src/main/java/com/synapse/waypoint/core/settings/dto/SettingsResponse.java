package com.synapse.waypoint.core.settings.dto;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/** {@code GET /api/settings} — see docs/api.md §2. */
public record SettingsResponse(LocalDate runDate, OffsetDateTime now, String timezone) {
}
