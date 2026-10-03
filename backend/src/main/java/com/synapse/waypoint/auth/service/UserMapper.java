package com.synapse.waypoint.auth.service;

import com.synapse.waypoint.auth.dto.UserResponse;
import com.synapse.waypoint.auth.entity.UserAccount;

final class UserMapper {

    private UserMapper() {
    }

    static UserResponse toResponse(UserAccount user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getRole(),
                user.getOutletId(),
                user.getDepot(),
                user.getVehicleId(),
                user.getLanguage(),
                user.getTheme());
    }
}
