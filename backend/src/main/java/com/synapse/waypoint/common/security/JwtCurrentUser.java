package com.synapse.waypoint.common.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;

/** Reads the current user from the verified login token - no database lookup per request. */
@Component
class JwtCurrentUser implements CurrentUser {

    @Override
    public String id() {
        return token().getSubject();
    }

    @Override
    public Optional<String> idIfSignedIn() {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken signedIn) {
            return Optional.ofNullable(signedIn.getToken().getSubject());
        }
        return Optional.empty();
    }

    @Override
    public Role role() {
        return Role.valueOf(token().getClaimAsString(TokenClaims.ROLE));
    }

    @Override
    public Optional<String> outletId() {
        return claim(TokenClaims.OUTLET_ID);
    }

    @Override
    public Optional<String> depot() {
        return claim(TokenClaims.DEPOT);
    }

    @Override
    public Optional<String> vehicleId() {
        return claim(TokenClaims.VEHICLE_ID);
    }

    private Optional<String> claim(String name) {
        return Optional.ofNullable(token().getClaimAsString(name));
    }

    private static Jwt token() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken();
        }
        throw new DomainException(ErrorCode.UNAUTHORIZED, "Please sign in.");
    }
}
