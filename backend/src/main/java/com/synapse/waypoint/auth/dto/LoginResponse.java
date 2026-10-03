package com.synapse.waypoint.auth.dto;

public record LoginResponse(String token, UserResponse user) {
}
