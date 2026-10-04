package com.synapse.waypoint.driver.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tools.jackson.core.JacksonException;
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
import com.synapse.waypoint.driver.entity.Conflict;
import com.synapse.waypoint.driver.entity.Delivery;
import com.synapse.waypoint.driver.entity.GoodsReturn;
import com.synapse.waypoint.driver.entity.StoreWait;
import com.synapse.waypoint.driver.entity.SyncLog;
import com.synapse.waypoint.driver.entity.SyncResult;
import com.synapse.waypoint.driver.entity.TripRun;
import com.synapse.waypoint.driver.entity.VehicleProblem;
import com.synapse.waypoint.driver.repository.ConflictRepository;
import com.synapse.waypoint.driver.repository.DeliveryRepository;
import com.synapse.waypoint.driver.repository.GoodsReturnRepository;
import com.synapse.waypoint.driver.repository.StoreWaitRepository;
import com.synapse.waypoint.driver.repository.SyncLogRepository;
import com.synapse.waypoint.driver.repository.TripRunRepository;
import com.synapse.waypoint.driver.repository.VehicleProblemRepository;

/**
 * Applies a phone's outbox.
 *
 * <p>The whole batch runs in one transaction and in the order the driver acted. Idempotency comes
 * from sync_log, whose primary key is the phone's own clientId: an item we have seen
 * before is answered DUPLICATE and skipped, so a request that succeeds on the server but loses its
 * response on the way back can be retried without recording a delivery twice.
 *
 * <p>One bad item does not sink the rest of the batch when the clash is a business one. A delivery
 * landing on a stop somebody else already delivered is answered CONFLICT and leaves a
 * conflicts row for the dispatcher (D8) - the phone has to be able to finish the rest of its
 * run. A payload the server refuses outright still throws, because dropping it quietly would lose
 * the driver's work without telling anyone.
 */
@Service
class DefaultSyncService implements SyncService {

    private final SyncLogRepository syncLog;
    private final DeliveryRepository deliveries;
    private final TripRunRepository tripRuns;
    private final StoreWaitRepository storeWaits;
    private final VehicleProblemRepository vehicleProblems;
    private final ConflictRepository conflicts;
    private final GoodsReturnRepository goodsReturns;
    private final OrderService orders;
    private final DemoClock clock;
    private final CurrentUser currentUser;
    private final ObjectMapper mapper;

    DefaultSyncService(SyncLogRepository syncLog, DeliveryRepository deliveries,
            TripRunRepository tripRuns, StoreWaitRepository storeWaits,
            VehicleProblemRepository vehicleProblems, ConflictRepository conflicts,
            GoodsReturnRepository goodsReturns, OrderService orders,
            DemoClock clock, CurrentUser currentUser, ObjectMapper mapper) {
        this.syncLog = syncLog;
        this.deliveries = deliveries;
        this.tripRuns = tripRuns;
        this.storeWaits = storeWaits;
        this.vehicleProblems = vehicleProblems;
        this.conflicts = conflicts;
        this.goodsReturns = goodsReturns;
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

            try {
                String entityId = apply(userId, item, now);
                syncLog.save(SyncLog.applied(item.clientId(), userId, item.type(), entityId, now));
                results.add(new SyncResponse.Result(item.clientId(), SyncResult.APPLIED, entityId));
            } catch (StopAlreadyDelivered clash) {
                // Physical facts win: the delivery already witnessed at this stop stands, and a
                // person decides the rest. The phone hears CONFLICT so it can show the card (R6).
                String conflictId = raiseConflict(userId, item, clash, now);
                syncLog.save(SyncLog.conflict(item.clientId(), userId, item.type(), conflictId, now));
                results.add(new SyncResponse.Result(item.clientId(), SyncResult.CONFLICT, conflictId));
            }
        }

        touchTrips(userId, now);
        return new SyncResponse(results);
    }

    /** Applies one action and returns the id of the row it produced. */
    private String apply(String userId, SyncItem item, Instant now) {
        return switch (item.type()) {
            case TRIP_ACCEPTED -> acceptTrip(userId, item);
            case ARRIVED -> arrived(userId, item, now);
            case DELIVERY_RECORDED -> recordDelivery(userId, item, now);
            case DELIVERY_UNDONE -> undoDelivery(item, now);
            case STORE_WAIT -> recordStoreWait(item, now);
            case VEHICLE_PROBLEM -> reportVehicleProblem(userId, item, now);
            case GOODS_RETURNED -> recordGoodsReturned(userId, item, now);
        };
    }

    /** R0 - the driver checked the load against the loader's list and accepted it. */
    private String acceptTrip(String userId, SyncItem item) {
        SyncItem.TripAccepted payload = convert(item, SyncItem.TripAccepted.class);
        TripRun run = tripRuns.findById(payload.tripId())
                .orElseGet(() -> tripRuns.save(TripRun.accepted(payload.tripId(), userId, clock.now())));
        return run.getTripId();
    }

    /**
     * R2 - the driver reached a stop. There is no separate arrivals table: the per-stop arrival time
     * that matters lives on the delivery deliveries.arrived_at, so a standalone ARRIVED
     * marks the trip as under way and is acknowledged.
     */
    private String arrived(String userId, SyncItem item, Instant now) {
        SyncItem.Arrived payload = convert(item, SyncItem.Arrived.class);
        Instant at = payload.arrivedAt() != null ? payload.arrivedAt() : now;
        tripsOf(userId).forEach(run -> run.start(at));
        return payload.stopId();
    }

    /**
     * R4 - a delivery at a stop. The order's status is changed through OrderService, never
     * here, so the lifecycle and the history row stay in one place.
     */
    private String recordDelivery(String userId, SyncItem item, Instant now) {
        SyncItem.DeliveryRecorded payload = convert(item, SyncItem.DeliveryRecorded.class);

        if (payload.outcome() != DeliveryOutcome.DELIVERED && payload.reason() == null) {
            // R4c and R4d: the driver picks why, so the record is the proof.
            throw new DomainException(ErrorCode.VALIDATION,
                    "a short or failed delivery needs a reason");
        }

        // Checked before the insert so a clash reads as a conflict and not as the database's unique
        // index on a live delivery at a stop.
        deliveries.findByStopIdAndUndoneAtIsNull(payload.stopId())
                .ifPresent(earlier -> {
                    throw new StopAlreadyDelivered(earlier);
                });

        Delivery delivery = Delivery.record(new Delivery.RecordedDelivery(
                newId(),
                payload.stopId(),
                payload.orderId(),
                payload.vehicleId() != null ? payload.vehicleId() : vehicleOf(),
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

    /** R4b - the 10-second undo. The row stays so the history is honest. */
    private String undoDelivery(SyncItem item, Instant now) {
        SyncItem.DeliveryUndone payload = convert(item, SyncItem.DeliveryUndone.class);
        Delivery delivery = deliveries.findById(payload.deliveryId())
                .orElseThrow(() -> new NotFoundException("delivery", payload.deliveryId()));
        delivery.undo(now);
        return delivery.getId();
    }

    /** R4w - the driver waited at a closed store. The wait is kept even if the delivery then worked. */
    private String recordStoreWait(SyncItem item, Instant now) {
        SyncItem.StoreWait payload = convert(item, SyncItem.StoreWait.class);
        StoreWait wait = StoreWait.record(new StoreWait.RecordedWait(
                newId(),
                payload.stopId(),
                payload.startedAt() != null ? payload.startedAt() : now,
                payload.endedAt(),
                payload.note(),
                item.clientId()));
        return storeWaits.save(wait).getId();
    }

    /** R9 - a problem reported from the cab, for dispatch to act on and answer in writing (R9ok). */
    private String reportVehicleProblem(String userId, SyncItem item, Instant now) {
        SyncItem.VehicleProblem payload = convert(item, SyncItem.VehicleProblem.class);
        VehicleProblem problem = VehicleProblem.report(new VehicleProblem.ReportedProblem(
                newId(),
                vehicleOf(),
                payload.tripId(),
                payload.kind(),
                payload.canDrive(),
                payload.fridgeTempC(),
                payload.unitsOnBoard(),
                payload.note(),
                userId,
                now,
                item.clientId()));
        return vehicleProblems.save(problem).getId();
    }

    /**
 * R8r - goods handed back at the depot at the end of a trip.
 *
 * <p>Recorded against the trip and the driver, idempotent on the item's clientId, so a driver who
 * records the handback twice after a dropped connection has still only handed the goods back once.
 */
    private String recordGoodsReturned(String userId, SyncItem item, Instant now) {
        SyncItem.GoodsReturned payload = convert(item, SyncItem.GoodsReturned.class);
        GoodsReturn returned = GoodsReturn.record(new GoodsReturn.RecordedReturn(
                newId(),
                payload.tripId(),
                payload.orderId(),
                payload.units(),
                payload.reason(),
                payload.signatureFileId(),
                userId,
                now,
                item.clientId()));
        return goodsReturns.save(returned).getId();
    }

    /**
     * Writes the row behind the dispatcher's decision card (D8). It carries both sides of the clash so
     * the card can explain itself without a second query.
     */
    private String raiseConflict(String userId, SyncItem item, StopAlreadyDelivered clash, Instant now) {
        SyncItem.DeliveryRecorded incoming = convert(item, SyncItem.DeliveryRecorded.class);
        Delivery earlier = clash.earlier();

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reason", "STOP_ALREADY_DELIVERED");
        details.put("rule", "physical facts win: the witnessed delivery stands");
        details.put("stopId", earlier.getStopId());
        details.put("existingDeliveryId", earlier.getId());
        details.put("existingOrderId", earlier.getOrderId());
        details.put("existingOutcome", earlier.getOutcome().name());
        details.put("existingUnits", earlier.getUnits());
        details.put("existingRecordedAt", earlier.getCompletedAt());
        details.put("existingRecordedBy", earlier.getRecordedBy());
        details.put("incomingDriverId", userId);
        details.put("incomingOrderId", incoming.orderId());
        details.put("incomingOutcome", incoming.outcome().name());
        details.put("incomingUnits", incoming.units());

        return conflicts.save(Conflict.raise(new Conflict.RecordedConflict(
                newId(), earlier.getStopId(), incoming.orderId(), earlier.getId(), details, now)))
                .getId();
    }

    /** Records that this driver's phone reached the server, so the live board can tell it from a gap. */
    private void touchTrips(String userId, Instant now) {
        tripsOf(userId).forEach(run -> run.syncedAt(now));
    }

    private List<TripRun> tripsOf(String userId) {
        return tripRuns.findAll().stream()
                .filter(run -> userId.equals(run.getDriverId()))
                .toList();
    }

    private String vehicleOf() {
        // A driver always works one vehicle, and the token carries it (CurrentUser).
        return currentUser.vehicleId().orElseThrow(() -> new DomainException(ErrorCode.VALIDATION,
                "this action needs a vehicleId"));
    }

    private String newId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Turns the untyped payload into the record for this action. A payload the server cannot read is
     * a client problem, so it is a 400 and not a 500 - the phone stops retrying and shows the item as
     * failed rather than looping on it forever.
     */
    private <T> T convert(SyncItem item, Class<T> type) {
        try {
            return mapper.convertValue(item.payload(), type);
        } catch (JacksonException unreadable) {
            throw new DomainException(ErrorCode.VALIDATION,
                    "cannot read a " + item.type() + " payload: " + unreadable.getOriginalMessage());
        }
    }

    /** Signals that the stop was already delivered. Not an error - a decision for a dispatcher. */
    private static final class StopAlreadyDelivered extends RuntimeException {

        private final Delivery earlier;

        StopAlreadyDelivered(Delivery earlier) {
            this.earlier = earlier;
        }

        Delivery earlier() {
            return earlier;
        }
    }
}