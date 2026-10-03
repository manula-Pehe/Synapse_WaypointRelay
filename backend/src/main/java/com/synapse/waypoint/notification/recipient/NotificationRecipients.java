package com.synapse.waypoint.notification.recipient;

import java.util.List;

import com.synapse.waypoint.common.security.Role;

/** Finds who a notification goes to. Implemented by the auth module, which owns the users. */
public interface NotificationRecipients {

    /** Ids of the active users of {@code role} inside {@code scope}. */
    List<String> activeUserIds(Role role, NotificationScope scope);

    boolean isActiveUser(String userId);
}
