package com.synapse.waypoint.auth.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.synapse.waypoint.auth.dto.LoginRequest;
import com.synapse.waypoint.auth.dto.LoginResponse;
import com.synapse.waypoint.auth.dto.UpdatePreferencesRequest;
import com.synapse.waypoint.auth.dto.UserResponse;
import com.synapse.waypoint.auth.service.AuthService;

/** Sign-in and the signed-in user's profile - docs/api.md §1. */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private final AuthService authService;

    AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    UserResponse me() {
        return authService.currentUser();
    }

    @PatchMapping("/me")
    UserResponse updatePreferences(@Valid @RequestBody UpdatePreferencesRequest request) {
        return authService.updatePreferences(request);
    }
}
