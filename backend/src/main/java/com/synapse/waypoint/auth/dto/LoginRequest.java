package com.synapse.waypoint.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * {@code identifier}: email (store, dispatcher), staff ID (driver) or depot (loader).
 * {@code secret}: password or PIN.
 */
public record LoginRequest(
        @NotBlank @Size(max = 120) String identifier,
        @NotBlank @Size(max = 100) String secret) {
}
