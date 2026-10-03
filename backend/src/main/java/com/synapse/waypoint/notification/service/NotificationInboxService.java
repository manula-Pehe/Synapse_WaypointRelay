package com.synapse.waypoint.notification.service;

import com.synapse.waypoint.notification.dto.NotificationListResponse;
import com.synapse.waypoint.notification.dto.NotificationResponse;
import com.synapse.waypoint.notification.dto.ReadAllResponse;

/** What the signed-in user can do with their own notifications. */
public interface NotificationInboxService {

    /** The user's notifications, newest first; only the unread ones when {@code unreadOnly}. */
    NotificationListResponse list(String userId, boolean unreadOnly);

    /** Marks one of the user's notifications read; another user's or an unknown id is "not found". */
    NotificationResponse markRead(String userId, String notificationId);

    ReadAllResponse markAllRead(String userId);
}
