package com.synapse.waypoint.notification.service;

import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.notification.entity.NotificationSeverity;
import com.synapse.waypoint.notification.recipient.NotificationScope;

/**
 * The one way every module sends a notification. Writes join the caller's transaction, so a
 * rolled-back business change leaves no notification behind.
 */
public interface NotificationService {

    /** Notifies one active user; throws {@code NotFoundException} when there is no such active user. */
    void notifyUser(String userId, NotificationSeverity severity, String type, String title, String body,
            String link);

    /** Notifies every active user of {@code role} inside {@code scope}, one row per user. */
    void notifyRole(Role role, NotificationScope scope, NotificationSeverity severity, String type, String title,
            String body, String link);
}
