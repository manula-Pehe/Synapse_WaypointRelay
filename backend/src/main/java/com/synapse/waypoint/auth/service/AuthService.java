package com.synapse.waypoint.auth.service;

import com.synapse.waypoint.auth.dto.LoginRequest;
import com.synapse.waypoint.auth.dto.LoginResponse;
import com.synapse.waypoint.auth.dto.UpdatePreferencesRequest;
import com.synapse.waypoint.auth.dto.UserResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

    UserResponse currentUser();

    UserResponse updatePreferences(UpdatePreferencesRequest request);
}
