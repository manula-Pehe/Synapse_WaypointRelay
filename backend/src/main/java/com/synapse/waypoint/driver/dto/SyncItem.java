package com.synapse.waypoint.driver.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import com.synapse.waypoint.core.order.entity.DeliveryOutcome;
import com.synapse.waypoint.driver.entity.DeliveryReason;
import com.synapse.waypoint.driver.entity.SyncActionType;
import com.synapse.waypoint.driver.entity.VehicleProblemKind;

/**
 * One action from the phone's outbox.
 *
 * <p>clientId was generated on the device and is the idempotency key: the server stores it
 * uniquely, so a batch that is sent twice - which happens every time a phone reconnects mid-request -
 * is applied once. payload stays untyped here because the shape depends on type; the
 * per-action records below are what it is converted to.
 */
public record SyncItem(
        @NotBlank String clientId,
        @NotNull SyncActionType type,
        @NotNull Instant createdAt,
        @NotNull Map<String, Object> payload) {

    /** R0 — the driver checked the load against the loader's list and accepted it. */
    public record TripAccepted(
            @NotBlank String tripId,
            String vehicleId,
            @PositiveOrZero Integer cases) {
    }

    /** R4 — a delivery recorded at a stop, with its proof. */
    public record DeliveryRecorded(
            @NotBlank String stopId,
            @NotBlank String orderId,
            String vehicleId,
            @NotNull DeliveryOutcome outcome,
            @PositiveOrZero int units,
            DeliveryReason reason,
            String receivedBy,
            String photoFileId,
            String signatureFileId,
            Instant arrivedAt,
            Instant completedAt) {
    }

    /** R4b — the driver's 10-second undo. */
    public record DeliveryUndone(
            @NotBlank String deliveryId,
            String stopId) {
    }

    /** A store the driver waited at (R4w). */
    public record StoreWait(
            @NotBlank String stopId,
            Instant startedAt,
            Instant endedAt,
            Integer minutes,
            String note) {
    }

    /** R9 — a vehicle problem reported from the cab. */
    public record VehicleProblem(
            String tripId,
            @NotNull VehicleProblemKind kind,
            boolean canDrive,
            BigDecimal fridgeTempC,
            /** Cases still on the truck, so the breakdown re-plan knows what it is moving (US-10.1). */
            @PositiveOrZero Integer unitsOnBoard,
            String note) {
    }

    /** R8r — goods handed back at the depot with a signature for responsibility. */
    public record GoodsReturned(
            @NotBlank String tripId,
            @NotBlank String orderId,
            @Positive int units,
            @NotNull DeliveryReason reason) {
    }

    /** R2 — the driver arrived at a stop. */
    public record Arrived(
            @NotBlank String stopId,
            Instant arrivedAt) {
    }

    /** Every action type this build can apply; used to reject an unknown type with a clear message. */
    public static final List<SyncActionType> SUPPORTED = List.of(
            SyncActionType.TRIP_ACCEPTED,
            SyncActionType.ARRIVED,
            SyncActionType.DELIVERY_RECORDED,
            SyncActionType.DELIVERY_UNDONE,
            SyncActionType.STORE_WAIT,
            SyncActionType.VEHICLE_PROBLEM,
            SyncActionType.GOODS_RETURNED);
}