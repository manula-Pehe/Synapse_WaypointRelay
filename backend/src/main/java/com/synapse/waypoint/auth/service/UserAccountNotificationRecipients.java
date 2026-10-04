package com.synapse.waypoint.auth.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.auth.entity.UserAccount;
import com.synapse.waypoint.auth.repository.UserAccountRepository;
import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.notification.recipient.NotificationRecipients;
import com.synapse.waypoint.notification.recipient.NotificationScope;

/** Answers "who gets this notification?" from the users table. */
@Component
@Transactional(readOnly = true)
class UserAccountNotificationRecipients implements NotificationRecipients {

    private final UserAccountRepository users;

    UserAccountNotificationRecipients(UserAccountRepository users) {
        this.users = users;
    }

    @Override
    public List<String> activeUserIds(Role role, NotificationScope scope) {
        return users.findByRoleAndActiveTrue(role).stream()
                .filter(user -> inScope(user, scope))
                .map(UserAccount::getId)
                .toList();
    }

    @Override
    public boolean isActiveUser(String userId) {
        return users.findById(userId).map(UserAccount::isActive).orElse(false);
    }

    private static boolean inScope(UserAccount user, NotificationScope scope) {
        return switch (user.getRole()) {
            case STORE_MANAGER -> matches(scope.outletId(), user.getOutletId());
            case DRIVER -> matches(scope.vehicleId(), user.getVehicleId());
            case LOADER -> matches(scope.depot(), user.getDepot());
            // A dispatcher without a depot works across all depots.
            case DISPATCHER -> user.getDepot() == null || matches(scope.depot(), user.getDepot());
        };
    }

    private static boolean matches(Optional<String> wanted, String actual) {
        return wanted.isEmpty() || wanted.get().equalsIgnoreCase(actual);
    }
}
