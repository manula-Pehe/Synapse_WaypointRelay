package com.synapse.waypoint.notification.dto;

import java.util.List;

/** {@code GET /api/notifications}: the list envelope plus how many of the user's notifications are unread. */
public record NotificationListResponse(List<NotificationResponse> items, int total, long unreadCount) {

    public NotificationListResponse {
        items = List.copyOf(items);
    }
}
