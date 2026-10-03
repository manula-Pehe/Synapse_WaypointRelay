package com.synapse.waypoint.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.JsonErrorWriter;

/**
 * HTTP security: stateless API, CORS, public health check, and the standard
 * {@code { code, message, details }} body for 401 and 403. Token authentication is added by the auth module.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/settings/clock").hasRole("DISPATCHER")
                        .anyRequest().authenticated())
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, ex) -> JsonErrorWriter.write(
                                response, ErrorCode.UNAUTHORIZED, "Please sign in."))
                        .accessDeniedHandler((request, response, ex) -> JsonErrorWriter.write(
                                response, ErrorCode.FORBIDDEN, "You don't have access to this.")));
        return http.build();
    }
}
