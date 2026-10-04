package com.synapse.waypoint.driver.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.security.Role;
import com.synapse.waypoint.core.order.entity.DeliveryOutcome;
import com.synapse.waypoint.driver.dto.ConflictDto;
import com.synapse.waypoint.driver.dto.DeliveryDto;
import com.synapse.waypoint.driver.dto.DriverStatusDto;
import com.synapse.waypoint.driver.dto.VehicleProblemDto;
import com.synapse.waypoint.driver.entity.Conflict;
import com.synapse.waypoint.driver.entity.ConflictStatus;
import com.synapse.waypoint.driver.entity.Delivery;
import com.synapse.waypoint.driver.entity.ProblemStatus;
import com.synapse.waypoint.driver.entity.TripRun;
import com.synapse.waypoint.driver.entity.VehicleProblem;
import com.synapse.waypoint.driver.repository.ConflictRepository;
import com.synapse.waypoint.driver.repository.DeliveryRepository;
import com.synapse.waypoint.driver.repository.TripRunRepository;
import com.synapse.waypoint.driver.repository.VehicleProblemRepository;

/**
 * The read side of the driver module.
 *
 * <p>Run dates are matched against the timestamps the driver actually recorded, because
 * deliveries, conflicts and vehicle_problems carry no run_date of
 * their own - the plan owns that. For a single depot's run this is the same answer, and it keeps
 * these queries off tables the driver module does not own.
 */
@Service
class DefaultDeliveryQueryService implements DeliveryQueryService {

    /** A driver whose phone has not reached the server in this long is shown as offline. */
    private static final Duration OFFLINE_AFTER = Duration.ofMinutes(5);

    private final DeliveryRepository deliveries;
    private final TripRunRepository tripRuns;
    private final ConflictRepository conflicts;
    private final VehicleProblemRepository vehicleProblems;
    private final DemoClock clock;
    private final CurrentUser currentUser;

    DefaultDeliveryQueryService(DeliveryRepository deliveries, TripRunRepository tripRuns,
            ConflictRepository conflicts, VehicleProblemRepository vehicleProblems, DemoClock clock,
            CurrentUser currentUser) {
        this.deliveries = deliveries;
        this.tripRuns = tripRuns;
        this.conflicts = conflicts;
        this.vehicleProblems = vehicleProblems;
        this.clock = clock;
        this.currentUser = currentUser;
    }

    /**
     * The vehicle a caller is allowed to ask about.
     *
     * A driver is pinned to its own vehicle however it spells the request; everyone else (dispatch on
     * the live board) may ask about any.
     */
    private String scopedVehicle(String requested) {
        if (!currentUser.is(Role.DRIVER)) {
            return requested;
        }
        return currentUser.vehicleId().orElseThrow(() -> new DomainException(ErrorCode.FORBIDDEN,
                "this account has no vehicle"));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<DeliveryDto> deliveryForOrder(String orderId) {
        return deliveries.findByOrderIdAndUndoneAtIsNull(orderId).map(DefaultDeliveryQueryService::toDto);
    }

    @Override
    @Transactional(readOnly = true)
    public DriverStatusDto driverStatus(String requestedVehicleId) {
        // Dispatch reads any vehicle for the live board, but a driver only ever reads its own. The
        // check is here rather than in the controller so every caller gets it, including other modules.
        String vehicleId = scopedVehicle(requestedVehicleId);
        // The live board asks about "now", so this one uses the current run date rather than a
        // supplied one; the rest are historical views.
        List<Delivery> todays = withinRunDate(
                deliveries.findByVehicleIdOrderByCompletedAtDesc(vehicleId),
                Delivery::getCompletedAt, clock.runDate());
        String driverId = todays.stream().findFirst().map(Delivery::getRecordedBy).orElse(null);
        Instant lastSyncAt = driverId == null ? null
                : tripsOf(driverId).stream()
                        .map(TripRun::getLastSyncAt)
                        .filter(Objects::nonNull)
                        .max(Instant::compareTo)
                        .orElse(null);

        return new DriverStatusDto(
                vehicleId,
                driverId,
                tripsOf(driverId).stream().findFirst().map(TripRun::getTripId).orElse(null),
                todays.size(),
                lastSyncAt,
                (int) vehicleProblems.findByVehicleIdOrderByReportedAtDesc(vehicleId).stream()
                        .filter(VehicleProblem::isOpen)
                        .count(),
                isOffline(lastSyncAt));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryDto> failedDeliveries(LocalDate runDate) {
        return withinRunDate(
                        deliveries.findByOutcomeNotAndUndoneAtIsNullOrderByCompletedAtDesc(
                                DeliveryOutcome.DELIVERED),
                        Delivery::getCompletedAt, runDate)
                .stream()
                .map(DefaultDeliveryQueryService::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConflictDto> openConflicts(LocalDate runDate) {
        return withinRunDate(conflicts.findByStatusOrderByCreatedAtDesc(ConflictStatus.OPEN),
                        Conflict::getCreatedAt, runDate).stream()
                .map(DefaultDeliveryQueryService::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleProblemDto> vehicleProblems(LocalDate runDate) {
        return withinRunDate(vehicleProblems.findAllByOrderByReportedAtDesc(),
                        VehicleProblem::getReportedAt, runDate).stream()
                .map(DefaultDeliveryQueryService::toDto)
                .toList();
    }

    private boolean isOffline(Instant lastSyncAt) {
        return lastSyncAt == null || lastSyncAt.isBefore(clock.now().minus(OFFLINE_AFTER));
    }

    private List<TripRun> tripsOf(String driverId) {
        if (driverId == null) {
            return List.of();
        }
        return tripRuns.findAll().stream()
                .filter(run -> driverId.equals(run.getDriverId()))
                .toList();
    }

    /** The business day, in the system time zone, as a half-open instant range. */
    private Instant dayStart(LocalDate runDate) {
        return runDate.atStartOfDay(DemoClock.ZONE).toInstant();
    }

    private <T> List<T> withinRunDate(List<T> all, Function<T, Instant> moment, LocalDate runDate) {
        Instant start = dayStart(runDate);
        Instant end = dayStart(runDate.plusDays(1));
        return all.stream()
                .filter(row -> {
                    Instant at = moment.apply(row);
                    return at != null && !at.isBefore(start) && at.isBefore(end);
                })
                .toList();
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
                delivery.isUndone(),
                delivery.getDecision(),
                delivery.getStoreChoice());
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

    private static VehicleProblemDto toDto(VehicleProblem problem) {
        return new VehicleProblemDto(
                problem.getId(),
                problem.getVehicleId(),
                problem.getTripId(),
                problem.getKind(),
                problem.isCanDrive(),
                problem.getFridgeTempC(),
                problem.getUnitsOnBoard(),
                problem.getNote(),
                problem.getStatus(),
                problem.getReply(),
                problem.getReportedAt());
    }
}