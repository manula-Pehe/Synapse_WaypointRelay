package com.synapse.waypoint.driver.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.entity.DeliveryOutcome;
import com.synapse.waypoint.driver.dto.ConflictDto;
import com.synapse.waypoint.driver.dto.DeliveryDto;
import com.synapse.waypoint.driver.entity.Conflict;
import com.synapse.waypoint.driver.entity.ConflictResolution;
import com.synapse.waypoint.driver.entity.ConflictStatus;
import com.synapse.waypoint.driver.entity.Delivery;
import com.synapse.waypoint.driver.entity.FailedDeliveryDecision;
import com.synapse.waypoint.driver.repository.ConflictRepository;
import com.synapse.waypoint.driver.repository.DeliveryRepository;

/** Settles the driver's offline work on the board (docs/api.md §8, F8 and F9). */
@Service
@Transactional
class DefaultDispatchDecisionService implements DispatchDecisionService {

    private final ConflictRepository conflicts;
    private final DeliveryRepository deliveries;
    private final CurrentUser currentUser;
    private final DemoClock clock;

    DefaultDispatchDecisionService(ConflictRepository conflicts, DeliveryRepository deliveries,
            CurrentUser currentUser, DemoClock clock) {
        this.conflicts = conflicts;
        this.deliveries = deliveries;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConflictDto> openConflicts(LocalDate runDate) {
        return conflicts.findByStatusOrderByCreatedAtDesc(ConflictStatus.OPEN).stream()
                .filter(conflict -> within(conflict.getCreatedAt(), runDate))
                .map(DefaultDispatchDecisionService::toDto)
                .toList();
    }

    @Override
    public ConflictDto resolveConflict(String conflictId, ConflictResolution resolution) {
        Conflict conflict = conflicts.findById(conflictId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "no such conflict"));
        if (conflict.getStatus() == ConflictStatus.RESOLVED) {
            // Answered twice is a no-op rather than an error, so a double-click is harmless.
            return toDto(conflict);
        }
        conflict.resolve(resolution == null ? ConflictResolution.KEEP_FIELD : resolution,
                currentUser.id(), clock.now());
        return toDto(conflicts.save(conflict));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryDto> failedDeliveries(LocalDate runDate) {
        // Every outcome except DELIVERED, filtered to the run day so the card is not a backlog.
        return deliveries
                .findByOutcomeNotAndUndoneAtIsNullOrderByCompletedAtDesc(DeliveryOutcome.DELIVERED).stream()
                .filter(delivery -> within(delivery.getCompletedAt(), runDate))
                .map(DefaultDispatchDecisionService::toDto)
                .toList();
    }

    @Override
    public DeliveryDto decideFailedDelivery(String deliveryId, FailedDeliveryDecision decision) {
        if (decision == null) {
            throw new DomainException(ErrorCode.VALIDATION,
                    "choose REPLAN_TOMORROW, TRY_LATER_TODAY or CANCEL");
        }
        Delivery delivery = deliveries.findById(deliveryId)
                .orElseThrow(() -> new DomainException(ErrorCode.NOT_FOUND, "no such delivery"));
        delivery.decide(decision, currentUser.id(), clock.now());
        return toDto(deliveries.save(delivery));
    }

    private boolean within(Instant at, LocalDate runDate) {
        if (at == null) {
            return false;
        }
        return !at.isBefore(runDate.atStartOfDay().toInstant(ZoneOffset.UTC))
                && at.isBefore(runDate.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
    }

    private static ConflictDto toDto(Conflict conflict) {
        return new ConflictDto(
                conflict.getId(),
                conflict.getStopId(),
                conflict.getOrderId(),
                conflict.getDeliveryId(),
                conflict.getDetails(),
                conflict.getStatus(),
                conflict.getResolution(),
                conflict.getCreatedAt());
    }

    private static DeliveryDto toDto(Delivery delivery) {
        return new DeliveryDto(
                delivery.getId(),
                delivery.getStopId(),
                delivery.getOrderId(),
                delivery.getVehicleId(),
                delivery.getOutcome(),
                delivery.getUnits(),
                delivery.getReason(),
                delivery.getReceivedBy(),
                delivery.getPhotoFileId() != null,
                delivery.getSignatureFileId() != null,
                delivery.getPhotoFileId(),
                delivery.getSignatureFileId(),
                delivery.getCompletedAt(),
                delivery.getUndoneAt() != null,
                delivery.getDecision(),
                delivery.getStoreChoice());
    }
}