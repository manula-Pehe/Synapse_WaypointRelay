package com.synapse.waypoint.auth.dto;

import jakarta.validation.constraints.Pattern;

/** {@code PATCH /api/auth/me} - omit a field to keep its current value. */
public record UpdatePreferencesRequest(
        @Pattern(regexp = "en|si|ta", message = "must be en, si or ta") String language,
        @Pattern(regexp = "system|light|dark", message = "must be system, light or dark") String theme) {
}
