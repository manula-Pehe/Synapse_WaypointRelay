package com.synapse.waypoint.auth.dto;

import com.synapse.waypoint.common.security.Role;

/** The {@code user} object of docs/api.md §1. */
public record UserResponse(
        String id,
        String name,
        Role role,
        String outletId,
        String depot,
        String vehicleId,
        String language,
        String theme) {
}
