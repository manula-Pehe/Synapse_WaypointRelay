package com.synapse.waypoint.auth.token;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import com.synapse.waypoint.auth.config.JwtProperties;
import com.synapse.waypoint.auth.entity.UserAccount;
import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.common.security.TokenClaims;

/**
 * Signed JWT carrying the user's role and scope (outlet, depot, vehicle), so requests need no user lookup.
 * Expiry uses real time, not the demo clock: a token's lifetime is a security matter.
 */
@Component
class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final Clock wallClock;

    JwtAccessTokenIssuer(JwtEncoder encoder, JwtProperties properties, Clock wallClock) {
        this.encoder = encoder;
        this.properties = properties;
        this.wallClock = wallClock;
    }

    @Override
    public String issue(UserAccount user) {
        Instant issuedAt = wallClock.instant();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(user.getId())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(lifetimeFor(user.getRole())))
                .claim(TokenClaims.ROLE, user.getRole().name())
                .claim(TokenClaims.NAME, user.getName());
        addIfPresent(claims, TokenClaims.OUTLET_ID, user.getOutletId());
        addIfPresent(claims, TokenClaims.DEPOT, user.getDepot());
        addIfPresent(claims, TokenClaims.VEHICLE_ID, user.getVehicleId());

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

    private Duration lifetimeFor(Role role) {
        return role == Role.DRIVER ? properties.driverTokenTtl() : properties.deskTokenTtl();
    }

    private static void addIfPresent(JwtClaimsSet.Builder claims, String name, String value) {
        if (value != null) {
            claims.claim(name, value);
        }
    }
}
