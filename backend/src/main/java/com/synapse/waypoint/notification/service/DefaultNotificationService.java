package com.synapse.waypoint.notification.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.notification.entity.Notification;
import com.synapse.waypoint.notification.entity.NotificationSeverity;
import com.synapse.waypoint.notification.recipient.NotificationRecipients;
import com.synapse.waypoint.notification.recipient.NotificationScope;
import com.synapse.waypoint.notification.repository.NotificationRepository;

@Service
@Transactional
class DefaultNotificationService implements NotificationService {

    private static final String ID_PREFIX = "ntf-";

    private final NotificationRepository notifications;
    private final NotificationRecipients recipients;
    private final DemoClock clock;

    DefaultNotificationService(NotificationRepository notifications, NotificationRecipients recipients,
            DemoClock clock) {
        this.notifications = notifications;
        this.recipients = recipients;
        this.clock = clock;
    }

    @Override
    public void notifyUser(String userId, NotificationSeverity severity, String type, String title, String body,
            String link) {
        requireContent(severity, type, title, body);
        if (!recipients.isActiveUser(userId)) {
            throw new NotFoundException("User", String.valueOf(userId));
        }
        notifications.save(newNotification(userId, severity, type, title, body, link));
    }

    @Override
    public void notifyRole(Role role, NotificationScope scope, NotificationSeverity severity, String type,
            String title, String body, String link) {
        requireContent(severity, type, title, body);
        notifications.saveAll(recipients.activeUserIds(role, scope).stream()
                .map(userId -> newNotification(userId, severity, type, title, body, link))
                .toList());
    }

    private Notification newNotification(String userId, NotificationSeverity severity, String type, String title,
            String body, String link) {
        return new Notification(ID_PREFIX + UUID.randomUUID(), userId, severity, type, title, body, link,
                clock.now());
    }

    private static void requireContent(NotificationSeverity severity, String type, String title, String body) {
        if (severity == null || isBlank(type) || isBlank(title) || isBlank(body)) {
            throw new DomainException(ErrorCode.VALIDATION, "A notification needs a severity, type, title and body.");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
