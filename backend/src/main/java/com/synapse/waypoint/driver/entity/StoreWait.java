package com.synapse.waypoint.driver.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A store the driver waited at store_waits, V20; screen R4w).
 *
 * <p>Written from the phone's outbox, so it carries the clientId that made it. The wait is
 * kept even when the delivery then succeeded, because "the store was shut for 20 minutes" is the
 * answer to why the run ran late.
 */
@Entity
@Table(name = "store_waits")
public class StoreWait {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "stop_id", nullable = false, length = 40)
    private String stopId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column
    private Integer minutes;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "client_id", nullable = false, length = 40, unique = true)
    private String clientId;

    protected StoreWait() {
        // for JPA
    }

    private StoreWait(RecordedWait recorded) {
        this.id = recorded.id();
        this.stopId = recorded.stopId();
        this.startedAt = recorded.startedAt();
        this.note = recorded.note();
        this.clientId = recorded.clientId();
        applyEnd(recorded.endedAt());
    }

    public static StoreWait record(RecordedWait recorded) {
        return new StoreWait(recorded);
    }

    /** Closes the wait. Minutes are worked out here so every caller records them the same way. */
    public void end(Instant endedAt) {
        applyEnd(endedAt);
    }

    private void applyEnd(Instant endedAt) {
        if (endedAt == null) {
            return;
        }
        this.endedAt = endedAt;
        this.minutes = (int) Math.max(0, (endedAt.toEpochMilli() - startedAt.toEpochMilli()) / 60_000);
    }

    public String getId() {
        return id;
    }

    public String getStopId() {
        return stopId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public Integer getMinutes() {
        return minutes;
    }

    public String getNote() {
        return note;
    }

    public String getClientId() {
        return clientId;
    }

    /** The values a queued STORE_WAIT carries. */
    public record RecordedWait(String id, String stopId, Instant startedAt, Instant endedAt,
            String note, String clientId) {
    }
}