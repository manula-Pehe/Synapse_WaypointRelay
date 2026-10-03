package com.synapse.waypoint.driver.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.databind.ObjectMapper;
import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.entity.DeliveryOutcome;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.driver.dto.SyncItem;
import com.synapse.waypoint.driver.dto.SyncRequest;
import com.synapse.waypoint.driver.dto.SyncResponse;
import com.synapse.waypoint.driver.entity.Delivery;
import com.synapse.waypoint.driver.entity.SyncActionType;
import com.synapse.waypoint.driver.entity.SyncLog;
import com.synapse.waypoint.driver.entity.SyncResult;
import com.synapse.waypoint.driver.entity.TripRun;
import com.synapse.waypoint.driver.repository.DeliveryRepository;
import com.synapse.waypoint.driver.repository.SyncLogRepository;
import com.synapse.waypoint.driver.repository.TripRunRepository;

/**
 * Applies a phone's outbox .
 *
 * <p>The whole batch runs in one transaction and in the order the driver acted. Idempotency comes
 * from sync_log}, whose primary key is the phone's own clientId: an item we have seen
 * before is answered DUPLICATE and skipped, so a request that succeeds on the server but loses its
 * response on the way back can be retried without recording a delivery twice.
 */
@Service
class DefaultSyncService implements SyncService {

    private final SyncLogRepository syncLog;
    private final DeliveryRepository deliveries;
    private final TripRunRepository tripRuns;
    private final OrderService orders;
    private final DemoClock clock;
    private final CurrentUser currentUser;
    private final ObjectMapper mapper;

    DefaultSyncService(SyncLogRepository syncLog, DeliveryRepository deliveries,
            TripRunRepository tripRuns, OrderService orders, DemoClock clock, CurrentUser currentUser,
            ObjectMapper mapper) {
        this.syncLog = syncLog;
        this.deliveries = deliveries;
        this.tripRuns = tripRuns;
        this.orders = orders;
        this.clock = clock;
        this.currentUser = currentUser;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public SyncResponse sync(String userId, SyncRequest request) {
        Instant now = clock.now();
        List<SyncResponse.Result> results = new ArrayList<>();

        for (SyncItem item : request.items()) {
            if (syncLog.existsById(item.clientId())) {
                results.add(new SyncResponse.Result(item.clientId(), SyncResult.DUPLICATE, null));
                continue;
            }

            String entityId = apply(userId, item, now);
            syncLog.save(SyncLog.applied(item.clientId(), userId, item.type(), entityId, now));
            results.add(new SyncResponse.Result(item.clientId(), SyncResult.APPLIED, entityId));
        }

        touchTrips(userId, now);
        return new SyncResponse(results);
    }

    /** Applies one action and returns the id of the row it produced. */
    private String apply(String userId, SyncItem item, Instant now) {
        return switch (item.type()) {
            case TRIP_ACCEPTED -> acceptTrip(userId, item);
            case DELIVERY_RECORDED -> recordDelivery(userId, item, now);
            case DELIVERY_UNDONE -> undoDelivery(item, now);
            case ARRIVED, STORE_WAIT, VEHICLE_PROBLEM ->
                throw new DomainException(ErrorCode.VALIDATION,
                        item.type() + " is not accepted yet");
        };
    }

    /** R0 — the driver checked the load against the loader's list and accepted it. */
    private String acceptTrip(String userId, SyncItem item) {
        SyncItem.TripAccepted payload = convert(item, SyncItem.TripAccepted.class);
        TripRun run = tripRuns.findById(payload.tripId())
                .orElseGet(() -> tripRuns.save(TripRun.accepted(payload.tripId(), userId, clock.now())));
        return run.getTripId();
    }

    /**
     * R4 — a delivery at a stop. The order's status is changed through OrderService, never
     * here, so the lifecycle and the history row stay in one place.
     */
    private String recordDelivery(String userId, SyncItem item, Instant now) {
        SyncItem.DeliveryRecorded payload = convert(item, SyncItem.DeliveryRecorded.class);
        String vehicleId = payload.vehicleId() != null ? payload.vehicleId() : vehicleOf(userId);

        Delivery delivery = Delivery.record(new Delivery.RecordedDelivery(
                UUID.randomUUID().toString(),
                payload.stopId(),
                payload.orderId(),
                vehicleId,
                payload.outcome(),
                payload.units(),
                payload.reason(),
                payload.receivedBy(),
                payload.photoFileId(),
                payload.signatureFileId(),
                payload.arrivedAt() != null ? payload.arrivedAt() : now,
                payload.completedAt() != null ? payload.completedAt() : now,
                userId,
                item.clientId()));

        if (delivery.hasReasonForShortfall()) {
            // R4c and R4d: the driver picks why, so the record is the proof.
            throw new DomainException(ErrorCode.VALIDATION,
                    "a short or failed delivery needs a reason");
        }

        deliveries.findByStopIdAndUndoneAtIsNull(payload.stopId()).ifPresent(earlier -> {
            throw new DomainException(ErrorCode.CONFLICT,
                    "this stop already has a delivery; it needs a dispatcher decision",
                    Map.of("stopId", payload.stopId(), "deliveryId", earlier.getId()));
        });

        deliveries.save(delivery);
        applyToOrder(payload);
        return delivery.getId();
    }

    /**
     * Hands the outcome to OrderService. A partial delivery also creates the remainder order
     * for what stayed on the truck (F6.1), so the store gets it on the next run.
     */
    private void applyToOrder(SyncItem.DeliveryRecorded payload) {
        orders.recordOutcome(payload.orderId(), payload.outcome(), payload.units());

        if (payload.outcome() == DeliveryOutcome.PARTIAL) {
            OrderDto order = orders.get(payload.orderId());
            int shortBy = order.units() - payload.units();
            if (shortBy > 0) {
                String reason = payload.reason() == null ? "PARTIAL" : payload.reason().name();
                orders.createRemainder(payload.orderId(), shortBy, reason);
            }
        }
    }

    /** R4b — the 10-second undo. The row stays so the history is honest. */
    private String undoDelivery(SyncItem item, Instant now) {
        SyncItem.DeliveryUndone payload = convert(item, SyncItem.DeliveryUndone.class);
        Delivery delivery = deliveries.findById(payload.deliveryId())
                .orElseThrow(() -> new NotFoundException("delivery", payload.deliveryId()));
        delivery.undo(now);
        return delivery.getId();
    }

    /** Records that this driver's phone reached the server, so the live board can tell it from a gap. */
    private void touchTrips(String userId, Instant now) {
        tripRuns.findAll().stream()
                .filter(run -> userId.equals(run.getDriverId()))
                .forEach(run -> run.syncedAt(now));
    }

    private String vehicleOf(String userId) {
        // A driver always works one vehicle, and the token carries it (CurrentUser).
        return currentUser.vehicleId().orElseThrow(() -> new DomainException(ErrorCode.VALIDATION,
                "this action needs a vehicleId"));
    }

    private <T> T convert(SyncItem item, Class<T> type) {
        return mapper.convertValue(item.payload(), type);
    }
}