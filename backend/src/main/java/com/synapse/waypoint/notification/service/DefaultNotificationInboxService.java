package com.synapse.waypoint.notification.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.notification.dto.NotificationListResponse;
import com.synapse.waypoint.notification.dto.NotificationResponse;
import com.synapse.waypoint.notification.dto.ReadAllResponse;
import com.synapse.waypoint.notification.entity.Notification;
import com.synapse.waypoint.notification.repository.NotificationRepository;

@Service
@Transactional
class DefaultNotificationInboxService implements NotificationInboxService {

    private final NotificationRepository notifications;
    private final DemoClock clock;

    DefaultNotificationInboxService(NotificationRepository notifications, DemoClock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponse list(String userId, boolean unreadOnly) {
        List<Notification> found = unreadOnly
                ? notifications.findByUserIdAndReadAtIsNullOrderByCreatedAtDescIdDesc(userId)
                : notifications.findByUserIdOrderByCreatedAtDescIdDesc(userId);
        List<NotificationResponse> items = found.stream().map(NotificationMapper::toResponse).toList();
        return new NotificationListResponse(items, items.size(), notifications.countByUserIdAndReadAtIsNull(userId));
    }

    @Override
    public NotificationResponse markRead(String userId, String notificationId) {
        Notification notification = notifications.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notification", notificationId));
        notification.markRead(clock.now());
        return NotificationMapper.toResponse(notification);
    }

    @Override
    public ReadAllResponse markAllRead(String userId) {
        return new ReadAllResponse(notifications.markAllRead(userId, clock.now()));
    }
}
