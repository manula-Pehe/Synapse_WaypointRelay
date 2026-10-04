package com.synapse.waypoint.driver.entity;

import java.time.Instant;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A driver's offline delivery that clashed with a change made on the board conflicts.
 *
 * <p>Created by the sync runner, never by the driver. The rule that shapes this row is
 * <em>physical facts win</em>: the delivery was witnessed at a shop door, so it stands and the
 * board's change is what gets pulled back. The conflict row exists so a human still sees the clash
 * and can reverse it deliberately - not so the system stays stuck.
 */
@Entity
@Table(name = "conflicts")
public class Conflict {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "stop_id", length = 40)
    private String stopId;

    @Column(name = "order_id", length = 40)
    private String orderId;

    @Column(name = "delivery_id", length = 40)
    private String deliveryId;

    /** What clashed, in enough detail for the D8 card to explain itself without a second query. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> details;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConflictStatus status = ConflictStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ConflictResolution resolution;

    @Column(name = "resolved_by", length = 40)
    private String resolvedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Conflict() {
        // for JPA
    }

    private Conflict(RecordedConflict recorded) {
        this.id = recorded.id();
        this.stopId = recorded.stopId();
        this.orderId = recorded.orderId();
        this.deliveryId = recorded.deliveryId();
        this.details = recorded.details();
        this.createdAt = recorded.createdAt();
    }

    public static Conflict raise(RecordedConflict recorded) {
        return new Conflict(recorded);
    }

    /**
     * The dispatcher settles it (D8m). Resolving does not undo the delivery — that already stands —
     * it records who chose to let it stand and when.
     */
    public void resolve(ConflictResolution chosen, String dispatcherId, Instant now) {
        this.resolution = chosen;
        this.resolvedBy = dispatcherId;
        this.resolvedAt = now;
        this.status = ConflictStatus.RESOLVED;
    }

    public boolean isOpen() {
        return status == ConflictStatus.OPEN;
    }

    public String getId() {
        return id;
    }

    public String getStopId() {
        return stopId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public Map<String, Object> getDetails() {
        return details;
    }

    public ConflictStatus getStatus() {
        return status;
    }

    public ConflictResolution getResolution() {
        return resolution;
    }

    public String getResolvedBy() {
        return resolvedBy;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    /** The values the sync runner records when a delivery clashes with the board. */
    public record RecordedConflict(String id, String stopId, String orderId, String deliveryId,
            Map<String, Object> details, Instant createdAt) {
    }
}