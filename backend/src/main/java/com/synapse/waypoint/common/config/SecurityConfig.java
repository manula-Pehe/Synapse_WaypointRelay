package com.synapse.waypoint.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.JsonErrorWriter;
import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.common.security.TokenClaims;

/**
 * HTTP security for the whole API: stateless bearer tokens, CORS, one role per area,
 * and the standard {@code { code, message, details }} body for 401 and 403.
 * Modules do not add rules here - new endpoints go under their area's path.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        AuthenticationEntryPoint unauthorized = (request, response, ex) ->
                JsonErrorWriter.write(response, ErrorCode.UNAUTHORIZED, "Please sign in.");
        AccessDeniedHandler forbidden = (request, response, ex) ->
                JsonErrorWriter.write(response, ErrorCode.FORBIDDEN, "You don't have access to this.");

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/settings").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/settings/clock").hasRole(Role.DISPATCHER.name())
                        .requestMatchers("/api/store/**").hasRole(Role.STORE_MANAGER.name())
                        .requestMatchers("/api/dispatch/**").hasRole(Role.DISPATCHER.name())
                        .requestMatchers("/api/loader/**").hasRole(Role.LOADER.name())
                        .requestMatchers("/api/driver/**").hasRole(Role.DRIVER.name())
                        .requestMatchers("/api/sync").hasAnyRole(Role.DRIVER.name(), Role.LOADER.name())
                        .anyRequest().authenticated())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(roleFromTokenConverter()))
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(unauthorized)
                        .accessDeniedHandler(forbidden));
        return http.build();
    }

    private static JwtAuthenticationConverter roleFromTokenConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName(TokenClaims.ROLE);
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
