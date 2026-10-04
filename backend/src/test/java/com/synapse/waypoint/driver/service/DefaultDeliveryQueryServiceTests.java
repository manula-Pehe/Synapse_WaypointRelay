package com.synapse.waypoint.driver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.entity.DeliveryOutcome;
import com.synapse.waypoint.driver.dto.ConflictDto;
import com.synapse.waypoint.driver.dto.DeliveryDto;
import com.synapse.waypoint.driver.dto.DriverStatusDto;
import com.synapse.waypoint.driver.dto.VehicleProblemDto;
import com.synapse.waypoint.driver.entity.Conflict;
import com.synapse.waypoint.driver.entity.ConflictStatus;
import com.synapse.waypoint.driver.entity.Delivery;
import com.synapse.waypoint.driver.entity.DeliveryReason;
import com.synapse.waypoint.driver.entity.TripRun;
import com.synapse.waypoint.driver.entity.VehicleProblem;
import com.synapse.waypoint.driver.entity.VehicleProblemKind;
import com.synapse.waypoint.driver.repository.ConflictRepository;
import com.synapse.waypoint.driver.repository.DeliveryRepository;
import com.synapse.waypoint.driver.repository.TripRunRepository;
import com.synapse.waypoint.driver.repository.VehicleProblemRepository;

/**
 * The read side other modules code against. The interesting rule is the offline
 * one: the live board has to be able to tell a driver with no signal from a driver who stopped.
 */
class DefaultDeliveryQueryServiceTests {

    private static final LocalDate RUN_DATE = LocalDate.parse("2026-10-01");
    private static final String DRIVER = "usr-nuwan";
    private static final String VEHICLE = "VEH036";
    /** 05:20 UTC is 10:50 in Colombo, comfortably inside the run date. */
    private static final Instant NOW = Instant.parse("2026-10-01T05:20:00Z");
    private static final Instant ANOTHER_DAY = NOW.plusSeconds(86_400);

    private DeliveryRepository deliveries;
    private TripRunRepository tripRuns;
    private ConflictRepository conflicts;
    private VehicleProblemRepository vehicleProblems;
    private DefaultDeliveryQueryService service;

    @BeforeEach
    void setUp() {
        deliveries = mock(DeliveryRepository.class);
        tripRuns = mock(TripRunRepository.class);
        conflicts = mock(ConflictRepository.class);
        vehicleProblems = mock(VehicleProblemRepository.class);

        DemoClock clock = mock(DemoClock.class);
        when(clock.now()).thenReturn(NOW);
        when(clock.runDate()).thenReturn(RUN_DATE);

        service = new DefaultDeliveryQueryService(deliveries, tripRuns, conflicts, vehicleProblems,
                clock);
    }

    @Test
    void theDeliveryForAnOrderCarriesItsProof() {
        Delivery delivery = delivery("dlv-1", "stp-1", "ord-1", DeliveryOutcome.DELIVERED, 42, NOW);
        when(deliveries.findByOrderIdAndUndoneAtIsNull("ord-1")).thenReturn(Optional.of(delivery));

        Optional<DeliveryDto> found = service.deliveryForOrder("ord-1");

        assertThat(found).isPresent();
        DeliveryDto dto = found.orElseThrow();
        assertThat(dto.id()).isEqualTo("dlv-1");
        assertThat(dto.units()).isEqualTo(42);
        assertThat(dto.receivedBy()).isEqualTo("Kasun");
        assertThat(dto.hasPhoto()).isTrue();
        assertThat(dto.hasSignature()).isTrue();
        assertThat(dto.isDecided()).isFalse();
    }

    @Test
    void anOrderWithNoDeliveryYieldsNothingRatherThanAFailure() {
        when(deliveries.findByOrderIdAndUndoneAtIsNull("ord-9")).thenReturn(Optional.empty());

        assertThat(service.deliveryForOrder("ord-9")).isEmpty();
    }

    @Test
    void onlyTheRunsOwnDayCountsTowardsItsStops() {
        when(deliveries.findByVehicleIdOrderByCompletedAtDesc(VEHICLE)).thenReturn(List.of(
                delivery("dlv-1", "stp-1", "ord-1", DeliveryOutcome.DELIVERED, 10, NOW),
                delivery("dlv-2", "stp-2", "ord-2", DeliveryOutcome.DELIVERED, 10, NOW),
                delivery("dlv-3", "stp-3", "ord-3", DeliveryOutcome.DELIVERED, 10, ANOTHER_DAY)));
        when(tripRuns.findAll()).thenReturn(List.of());
        when(vehicleProblems.findByVehicleIdOrderByReportedAtDesc(VEHICLE)).thenReturn(List.of());

        DriverStatusDto status = service.driverStatus(VEHICLE);

        assertThat(status.stopsDone()).isEqualTo(2);
        assertThat(status.driverId()).isEqualTo(DRIVER);
        assertThat(status.openProblems()).isZero();
    }

    /** The one the live board depends on: no signal is not the same as no work. */
    @Test
    void aDriverWhosePhoneHasNotReachedTheServerIsShownAsOffline() {
        givenAVehicleThatLastSyncedAt(NOW.minusSeconds(600));

        assertThat(service.driverStatus(VEHICLE).offline()).isTrue();
    }

    @Test
    void aDriverWhoJustSyncedIsNotOffline() {
        givenAVehicleThatLastSyncedAt(NOW.minusSeconds(30));

        DriverStatusDto status = service.driverStatus(VEHICLE);
        assertThat(status.offline()).isFalse();
        assertThat(status.lastSyncAt()).isEqualTo(NOW.minusSeconds(30));
    }

    @Test
    void aVehicleThatHasNeverSyncedIsOfflineRatherThanMissing() {
        when(deliveries.findByVehicleIdOrderByCompletedAtDesc(VEHICLE)).thenReturn(List.of());
        when(tripRuns.findAll()).thenReturn(List.of());
        when(vehicleProblems.findByVehicleIdOrderByReportedAtDesc(VEHICLE)).thenReturn(List.of());

        DriverStatusDto status = service.driverStatus(VEHICLE);

        assertThat(status.offline()).isTrue();
        assertThat(status.driverId()).isNull();
        assertThat(status.lastSyncAt()).isNull();
    }

    @Test
    void openProblemsAreCountedOnTheDriversStatus() {
        when(deliveries.findByVehicleIdOrderByCompletedAtDesc(VEHICLE)).thenReturn(List.of(
                delivery("dlv-1", "stp-1", "ord-1", DeliveryOutcome.DELIVERED, 10, NOW)));
        when(tripRuns.findAll()).thenReturn(List.of());
        when(vehicleProblems.findByVehicleIdOrderByReportedAtDesc(VEHICLE)).thenReturn(List.of(
                problem("prb-1", NOW), problem("prb-2", NOW), resolvedProblem("prb-3", NOW)));

        assertThat(service.driverStatus(VEHICLE).openProblems()).isEqualTo(2);
    }

    @Test
    void onlyTheShortAndFailedDeliveriesGoToTheDispatchersDecisionList() {
        when(deliveries.findByOutcomeNotAndUndoneAtIsNullOrderByCompletedAtDesc(
                DeliveryOutcome.DELIVERED)).thenReturn(List.of(
                delivery("dlv-1", "stp-1", "ord-1", DeliveryOutcome.FAILED, 0, NOW,
                        DeliveryReason.STORE_CLOSED),
                delivery("dlv-2", "stp-2", "ord-2", DeliveryOutcome.PARTIAL, 30, NOW,
                        DeliveryReason.DAMAGED),
                delivery("dlv-3", "stp-3", "ord-3", DeliveryOutcome.FAILED, 0, ANOTHER_DAY,
                        DeliveryReason.REFUSED)));

        List<DeliveryDto> failed = service.failedDeliveries(RUN_DATE);

        assertThat(failed).extracting(DeliveryDto::id).containsExactly("dlv-1", "dlv-2");
        assertThat(failed.get(0).reason()).isEqualTo(DeliveryReason.STORE_CLOSED);
    }

    @Test
    void onlyTodaysConflictsAreWaitingOnTheDispatcher() {
        when(conflicts.findByStatusOrderByCreatedAtDesc(ConflictStatus.OPEN)).thenReturn(List.of(
                conflict("cft-1", "stp-1", NOW), conflict("cft-2", "stp-2", ANOTHER_DAY)));

        List<ConflictDto> open = service.openConflicts(RUN_DATE);

        assertThat(open).extracting(ConflictDto::id).containsExactly("cft-1");
        assertThat(open.get(0).details()).containsEntry("reason", "STOP_ALREADY_DELIVERED");
    }

    @Test
    void vehicleProblemsComeBackNewestFirstAndOnlyForThatRun() {
        when(vehicleProblems.findAllByOrderByReportedAtDesc()).thenReturn(List.of(
                problem("prb-2", NOW.minusSeconds(60)), problem("prb-1", NOW),
                problem("prb-3", ANOTHER_DAY)));

        List<VehicleProblemDto> problems = service.vehicleProblems(RUN_DATE);

        assertThat(problems).extracting(VehicleProblemDto::id).containsExactly("prb-2", "prb-1");
        assertThat(problems.get(0).kind()).isEqualTo(VehicleProblemKind.TYRE);
        assertThat(problems.get(0).canDrive()).isTrue();
    }

    private void givenAVehicleThatLastSyncedAt(Instant lastSyncAt) {
        TripRun run = TripRun.accepted("trp-1", DRIVER, NOW.minusSeconds(3_600));
        run.syncedAt(lastSyncAt);
        when(deliveries.findByVehicleIdOrderByCompletedAtDesc(VEHICLE)).thenReturn(List.of(
                delivery("dlv-1", "stp-1", "ord-1", DeliveryOutcome.DELIVERED, 10, NOW)));
        when(tripRuns.findAll()).thenReturn(List.of(run));
        when(vehicleProblems.findByVehicleIdOrderByReportedAtDesc(VEHICLE)).thenReturn(List.of());
    }

    private Delivery delivery(String id, String stopId, String orderId, DeliveryOutcome outcome,
            int units, Instant at) {
        return delivery(id, stopId, orderId, outcome, units, at, null);
    }

    private Delivery delivery(String id, String stopId, String orderId, DeliveryOutcome outcome,
            int units, Instant at, DeliveryReason reason) {
        return Delivery.record(new Delivery.RecordedDelivery(id, stopId, orderId, VEHICLE, outcome,
                units, reason, "Kasun", "file-photo", "file-signature", at, at, DRIVER, "c-" + id));
    }

    private VehicleProblem problem(String id, Instant at) {
        return VehicleProblem.report(new VehicleProblem.ReportedProblem(id, VEHICLE, "trp-1",
                VehicleProblemKind.TYRE, true, null, "slow leak", DRIVER, at, "c-" + id));
    }

    private VehicleProblem resolvedProblem(String id, Instant at) {
        VehicleProblem problem = problem(id, at);
        problem.resolve(at);
        return problem;
    }

    private Conflict conflict(String id, String stopId, Instant at) {
        return Conflict.raise(new Conflict.RecordedConflict(id, stopId, "ord-1", "dlv-1",
                Map.of("reason", "STOP_ALREADY_DELIVERED", "existingUnits", 20), at));
    }
}