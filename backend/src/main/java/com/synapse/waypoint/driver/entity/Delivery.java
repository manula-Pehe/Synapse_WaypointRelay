package com.synapse.waypoint.driver.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.synapse.waypoint.core.order.entity.DeliveryOutcome;

/**
 * What the driver did at one stop.
 *
 * <p>A delivery is written from the phone's outbox and therefore carries the clientId that
 * made it, so a replayed batch cannot record it twice. Changing an order's status is not done here -
 * that is OrderService's job, and the sync runner calls it after saving this row.
 */
@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    @Column(length = 40)
    private String id;

    @Column(name = "stop_id", nullable = false, length = 40)
    private String stopId;

    @Column(name = "order_id", nullable = false, length = 40)
    private String orderId;

    @Column(name = "vehicle_id", nullable = false, length = 10)
    private String vehicleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryOutcome outcome;

    @Column(nullable = false)
    private int units;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DeliveryReason reason;

    @Column(name = "received_by", length = 100)
    private String receivedBy;

    @Column(name = "photo_file_id", length = 40)
    private String photoFileId;

    @Column(name = "signature_file_id", length = 40)
    private String signatureFileId;

    @Column(name = "arrived_at", nullable = false)
    private Instant arrivedAt;

    @Column(name = "completed_at", nullable = false)
    private Instant completedAt;

    @Column(name = "recorded_by", length = 40)
    private String recordedBy;

    @Column(name = "client_id", nullable = false, length = 40, unique = true)
    private String clientId;

    /** Set by the driver's 10-second undo (R4b). The row stays, so the history is honest. */
    @Column(name = "undone_at")
    private Instant undoneAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private FailedDeliveryDecision decision;

    @Column(name = "decided_by", length = 40)
    private String decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "store_choice", length = 20)
    private FailedDeliveryDecision storeChoice;

    @Column(name = "store_choice_at")
    private Instant storeChoiceAt;

    /** The driver records this and the dispatcher decides on it, so two users write the row. */
    @Version
    @Column(nullable = false)
    private Integer version;

    protected Delivery() {
        // for JPA
    }

    private Delivery(RecordedDelivery recorded) {
        this.id = recorded.id();
        this.stopId = recorded.stopId();
        this.orderId = recorded.orderId();
        this.vehicleId = recorded.vehicleId();
        this.outcome = recorded.outcome();
        this.units = recorded.units();
        this.reason = recorded.reason();
        this.receivedBy = recorded.receivedBy();
        this.photoFileId = recorded.photoFileId();
        this.signatureFileId = recorded.signatureFileId();
        this.arrivedAt = recorded.arrivedAt();
        this.completedAt = recorded.completedAt();
        this.recordedBy = recorded.recordedBy();
        this.clientId = recorded.clientId();
    }

    public static Delivery record(RecordedDelivery recorded) {
        return new Delivery(recorded);
    }

    /** A short or failed delivery must say why - the record is the proof (F6). */
    public boolean hasReasonForShortfall() {
        return outcome != DeliveryOutcome.DELIVERED && reason == null;
    }

    public void undo(Instant now) {
        undoneAt = now;
    }

    public boolean isUndone() {
        return undoneAt != null;
    }

    /** The dispatcher's call on a failed delivery (D6f). */
    public void decide(FailedDeliveryDecision chosen, String dispatcherId, Instant now) {
        decision = chosen;
        decidedBy = dispatcherId;
        decidedAt = now;
    }

    /** The store's preference, which the dispatcher may see but not overwrite (S3f). */
    public void recordStoreChoice(FailedDeliveryDecision chosen, Instant now) {
        storeChoice = chosen;
        storeChoiceAt = now;
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

    public String getVehicleId() {
        return vehicleId;
    }

    public DeliveryOutcome getOutcome() {
        return outcome;
    }

    public int getUnits() {
        return units;
    }

    public DeliveryReason getReason() {
        return reason;
    }

    public String getReceivedBy() {
        return receivedBy;
    }

    public String getPhotoFileId() {
        return photoFileId;
    }

    public String getSignatureFileId() {
        return signatureFileId;
    }

    public Instant getArrivedAt() {
        return arrivedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public String getClientId() {
        return clientId;
    }

    public Instant getUndoneAt() {
        return undoneAt;
    }

    public FailedDeliveryDecision getDecision() {
        return decision;
    }

    public String getDecidedBy() {
        return decidedBy;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public FailedDeliveryDecision getStoreChoice() {
        return storeChoice;
    }

    public Instant getStoreChoiceAt() {
        return storeChoiceAt;
    }

    public Integer getVersion() {
        return version;
    }

    /** The values a queued {@code DELIVERY_RECORDED} carries. */
    public record RecordedDelivery(String id, String stopId, String orderId, String vehicleId,
            DeliveryOutcome outcome, int units, DeliveryReason reason, String receivedBy,
            String photoFileId, String signatureFileId, Instant arrivedAt, Instant completedAt,
            String recordedBy, String clientId) {
    }
}