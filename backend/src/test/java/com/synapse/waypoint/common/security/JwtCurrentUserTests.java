package com.synapse.waypoint.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class JwtCurrentUserTests {

    private final JwtCurrentUser currentUser = new JwtCurrentUser();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnTheSubjectWhenSignedInWithAToken() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(token("usr-1")));

        assertThat(currentUser.idIfSignedIn()).contains("usr-1");
    }

    @Test
    void shouldBeEmptyWithoutThrowingWhenNobodyIsSignedIn() {
        assertThat(currentUser.idIfSignedIn()).isEmpty();
    }

    @Test
    void shouldBeEmptyWhenTheAuthenticationIsNotATokenLogin() {
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("job", "n/a"));

        assertThat(currentUser.idIfSignedIn()).isEmpty();
    }

    private static Jwt token(String subject) {
        return new Jwt("t", Instant.now(), Instant.now().plusSeconds(60), Map.of("alg", "HS256"),
                Map.of("sub", subject));
    }
}
