package com.synapse.waypoint.auth.token;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.synapse.waypoint.auth.config.JwtProperties;
import com.synapse.waypoint.auth.entity.UserAccount;
import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.common.security.TokenClaims;

class JwtAccessTokenIssuerTests {

    private static final String SECRET = "unit-test-secret-unit-test-secret-0123456789";
    private static final Duration DESK = Duration.ofHours(12);
    private static final Duration DRIVER = Duration.ofHours(16);

    private final SecretKeySpec key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    private final JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    private final Instant issuedAt = Instant.now();
    private final JwtAccessTokenIssuer issuer = new JwtAccessTokenIssuer(
            new NimbusJwtEncoder(new ImmutableSecret<>(key)),
            new JwtProperties(SECRET, DESK, DRIVER),
            Clock.fixed(issuedAt, ZoneOffset.UTC));

    @Test
    void shouldCarryRoleAndScopeOfAStoreManager() {
        Jwt token = decoder.decode(issuer.issue(user("usr-1", Role.STORE_MANAGER, "OUT900", null, null)));

        assertThat(token.getSubject()).isEqualTo("usr-1");
        assertThat(token.getClaimAsString(TokenClaims.ROLE)).isEqualTo("STORE_MANAGER");
        assertThat(token.getClaimAsString(TokenClaims.OUTLET_ID)).isEqualTo("OUT900");
        assertThat(token.hasClaim(TokenClaims.VEHICLE_ID)).isFalse();
    }

    @Test
    void shouldGiveDriversALongerTokenThanDeskRoles() {
        Jwt desk = decoder.decode(issuer.issue(user("usr-2", Role.DISPATCHER, null, null, null)));
        Jwt driver = decoder.decode(issuer.issue(user("usr-3", Role.DRIVER, null, null, "VEH900")));

        assertThat(Duration.between(desk.getIssuedAt(), desk.getExpiresAt())).isEqualTo(DESK);
        assertThat(Duration.between(driver.getIssuedAt(), driver.getExpiresAt())).isEqualTo(DRIVER);
    }

    private static UserAccount user(String id, Role role, String outletId, String depot, String vehicleId) {
        UserAccount user = mock(UserAccount.class);
        when(user.getId()).thenReturn(id);
        when(user.getName()).thenReturn("Test User");
        when(user.getRole()).thenReturn(role);
        when(user.getOutletId()).thenReturn(outletId);
        when(user.getDepot()).thenReturn(depot);
        when(user.getVehicleId()).thenReturn(vehicleId);
        return user;
    }
}
