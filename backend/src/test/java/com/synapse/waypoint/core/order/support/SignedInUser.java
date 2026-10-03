package com.synapse.waypoint.core.order.support;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.common.security.TokenClaims;

/** Puts a signed-in user in the security context, the way a verified login token would. */
public final class SignedInUser {

    private SignedInUser() {
    }

    public static void asStoreManager(String userId, String outletId) {
        signIn(userId, Role.STORE_MANAGER, Map.of(TokenClaims.OUTLET_ID, outletId));
    }

    public static void asDispatcher(String userId) {
        signIn(userId, Role.DISPATCHER, Map.of());
    }

    public static void signOut() {
        SecurityContextHolder.clearContext();
    }

    private static void signIn(String userId, Role role, Map<String, Object> extraClaims) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put("sub", userId);
        claims.put(TokenClaims.ROLE, role.name());
        Jwt jwt = new Jwt("token", Instant.now(), Instant.now().plusSeconds(600), Map.of("alg", "HS256"), claims);
        SecurityContextHolder.getContext().setAuthentication(
                new JwtAuthenticationToken(jwt, java.util.List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }
}
