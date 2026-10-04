package com.synapse.waypoint.notification.entity;

import java.time.Instant;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One notification for one recipient (table {@code notifications}, V2). */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "user_id", nullable = false, length = 40)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private NotificationSeverity severity;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(length = 200)
    private String link;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    protected Notification() {
        // for JPA
    }

    public Notification(String id, String userId, NotificationSeverity severity, String type, String title,
            String body, String link, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.severity = severity;
        this.type = type;
        this.title = title;
        this.body = body;
        this.link = link;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public NotificationSeverity getSeverity() {
        return severity;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public String getLink() {
        return link;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Optional<Instant> getReadAt() {
        return Optional.ofNullable(readAt);
    }

    /** Marks the notification read; reading it again keeps the first read time. */
    public void markRead(Instant at) {
        if (readAt == null) {
            readAt = at;
        }
    }
}
