package com.synapse.waypoint.notification.entity;

/** How urgent a notification is (docs/api.md, Enumerations). CRITICAL can never be muted. */
public enum NotificationSeverity {
    CRITICAL,
    WARNING,
    INFO
}
