package com.synapse.waypoint.notification.dto;

import java.time.OffsetDateTime;

import com.synapse.waypoint.notification.entity.NotificationSeverity;

/** A notification as the bell shows it - docs/api.md §5. */
public record NotificationResponse(String id, NotificationSeverity severity, String type, String title, String body,
        String link, OffsetDateTime createdAt, OffsetDateTime readAt) {
}
