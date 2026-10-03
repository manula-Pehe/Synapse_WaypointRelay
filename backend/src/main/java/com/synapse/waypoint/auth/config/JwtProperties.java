package com.synapse.waypoint.auth.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Token settings. The secret comes from {@code JWT_SECRET}; startup fails if it is missing or short,
 * so the app never runs with a weak signing key.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration deskTokenTtl, Duration driverTokenTtl) {

    static final int MIN_SECRET_LENGTH = 32;

    public JwtProperties {
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set and at least " + MIN_SECRET_LENGTH + " characters long");
        }
    }
}
