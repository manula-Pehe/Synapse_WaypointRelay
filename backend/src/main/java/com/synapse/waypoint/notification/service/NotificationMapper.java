package com.synapse.waypoint.notification.service;

import com.synapse.waypoint.common.time.ApiTimestamp;
import com.synapse.waypoint.notification.dto.NotificationResponse;
import com.synapse.waypoint.notification.entity.Notification;

final class NotificationMapper {

    private NotificationMapper() {
    }

    static NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getSeverity(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getLink(),
                ApiTimestamp.of(notification.getCreatedAt()),
                notification.getReadAt().map(ApiTimestamp::of).orElse(null));
    }
}
