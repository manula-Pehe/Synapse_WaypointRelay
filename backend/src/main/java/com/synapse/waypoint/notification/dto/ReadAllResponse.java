package com.synapse.waypoint.notification.dto;

/** {@code POST /api/notifications/read-all}: how many notifications were newly marked read. */
public record ReadAllResponse(int updated) {
}
