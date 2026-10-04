package com.synapse.waypoint.driver.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 <p>The primary key is the client's own clientId, and that is the whole offline guarantee:
 * applying the same action twice inserts this row once, so the second attempt is a no-op and the
 * phone gets SyncResult#DUPLICATE back. Nothing else in the system needs to know that the
 * phone was offline.
 */
@Entity
@Table(name = "sync_log")
public class SyncLog {

    @Id
    @Column(name = "client_id", length = 40)
    private String clientId;

    @Column(name = "user_id", length = 40)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SyncActionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SyncResult result;

    /** The row the action produced, so a client can follow up without guessing. */
    @Column(name = "entity_id", length = 40)
    private String entityId;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected SyncLog() {
        // for JPA
    }

    private SyncLog(String clientId, String userId, SyncActionType type, SyncResult result,
            String entityId, Instant receivedAt) {
        this.clientId = clientId;
        this.userId = userId;
        this.type = type;
        this.result = result;
        this.entityId = entityId;
        this.receivedAt = receivedAt;
    }

    public static SyncLog applied(String clientId, String userId, SyncActionType type, String entityId,
            Instant receivedAt) {
        return new SyncLog(clientId, userId, type, SyncResult.APPLIED, entityId, receivedAt);
    }

    public static SyncLog duplicate(String clientId, String userId, SyncActionType type, Instant receivedAt) {
        return new SyncLog(clientId, userId, type, SyncResult.DUPLICATE, null, receivedAt);
    }

    public static SyncLog conflict(String clientId, String userId, SyncActionType type, String entityId,
            Instant receivedAt) {
        return new SyncLog(clientId, userId, type, SyncResult.CONFLICT, entityId, receivedAt);
    }

    public String getClientId() {
        return clientId;
    }

    public String getUserId() {
        return userId;
    }

    public SyncActionType getType() {
        return type;
    }

    public SyncResult getResult() {
        return result;
    }

    public String getEntityId() {
        return entityId;
    }

    public Instant getReceivedAt() {
        return receivedAt;
    }
}