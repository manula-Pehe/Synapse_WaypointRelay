package com.synapse.waypoint.driver.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Goods handed back at the depot at the end of a trip (returns, V20; screen R8r).
 *
 * <p>Written from the phone's outbox, so it carries the clientId that made it — a driver who records
 * the handback twice after a dropped connection has still only handed the goods back once.
 *
 * <p>Stock coming back to the shelf, not an outcome on the order: the order keeps its own status and
 * the shortfall stays outstanding, so the cases get delivered on a later run.
 *
 * <p>signatureFileId is who took responsibility for the handback (US-11.2). Optional: a handback
 * queued from a phone out of reach of the depot cannot capture one, and losing the record of the
 * handback over a missing signature would be the worse outcome.
 */
@Entity
@Table(name = "returns")
public class GoodsReturn {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "trip_id", nullable = false, length = 40)
    private String tripId;

    @Column(name = "order_id", nullable = false, length = 40)
    private String orderId;

    @Column(nullable = false)
    private Integer units;

    @Column(name = "signature_file_id", length = 40)
    private String signatureFileId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DeliveryReason reason;

    @Column(name = "recorded_by", length = 40)
    private String recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "client_id", nullable = false, length = 40, unique = true)
    private String clientId;

    protected GoodsReturn() {
        // for JPA
    }

    private GoodsReturn(RecordedReturn recorded) {
        this.id = recorded.id();
        this.tripId = recorded.tripId();
        this.orderId = recorded.orderId();
        this.units = recorded.units();
        this.signatureFileId = recorded.signatureFileId();
        this.reason = recorded.reason();
        this.recordedBy = recorded.recordedBy();
        this.recordedAt = recorded.recordedAt();
        this.clientId = recorded.clientId();
    }

    public static GoodsReturn record(RecordedReturn recorded) {
        return new GoodsReturn(recorded);
    }

    public String getId() {
        return id;
    }

    public String getTripId() {
        return tripId;
    }

    public String getOrderId() {
        return orderId;
    }

    public Integer getUnits() {
        return units;
    }

    /** The signature captured at the depot, or null when the phone could not reach one. */
    public String getSignatureFileId() {
        return signatureFileId;
    }

    public DeliveryReason getReason() {
        return reason;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }

    public String getClientId() {
        return clientId;
    }

    /** The values a queued GOODS_RETURNED carries. */
    public record RecordedReturn(String id, String tripId, String orderId, int units,
            DeliveryReason reason, String signatureFileId, String recordedBy, Instant recordedAt,
            String clientId) {
    }
}
