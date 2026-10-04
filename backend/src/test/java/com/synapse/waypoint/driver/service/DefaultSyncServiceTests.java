package com.synapse.waypoint.driver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.security.CurrentUser;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.entity.DeliveryOutcome;
import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.service.OrderService;
import com.synapse.waypoint.driver.dto.SyncItem;
import com.synapse.waypoint.driver.dto.SyncRequest;
import com.synapse.waypoint.driver.dto.SyncResponse;
import com.synapse.waypoint.driver.entity.Conflict;
import com.synapse.waypoint.driver.entity.Delivery;
import com.synapse.waypoint.driver.entity.DeliveryReason;
import com.synapse.waypoint.driver.entity.GoodsReturn;
import com.synapse.waypoint.driver.entity.StoreWait;
import com.synapse.waypoint.driver.entity.SyncActionType;
import com.synapse.waypoint.driver.entity.SyncLog;
import com.synapse.waypoint.driver.entity.SyncResult;
import com.synapse.waypoint.driver.entity.TripRun;
import com.synapse.waypoint.driver.entity.VehicleProblem;
import com.synapse.waypoint.driver.entity.VehicleProblemKind;
import com.synapse.waypoint.driver.repository.ConflictRepository;
import com.synapse.waypoint.driver.repository.DeliveryRepository;
import com.synapse.waypoint.driver.repository.GoodsReturnRepository;
import com.synapse.waypoint.driver.repository.StoreWaitRepository;
import com.synapse.waypoint.driver.repository.SyncLogRepository;
import com.synapse.waypoint.driver.repository.TripRunRepository;
import com.synapse.waypoint.driver.repository.VehicleProblemRepository;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * The offline guarantee: an action that arrives twice is carried out once.
 *
 * <p>These run without a database - the repositories are mocked — so the rule behind the whole sync
 * design can be checked on any machine. Persistence is exercised by the database-backed tests in CI.
 */
class DefaultSyncServiceTests {

    private static final String DRIVER = "usr-nuwan";
    private static final String VEHICLE = "VEH036";
    private static final Instant NOW = Instant.parse("2026-10-01T05:20:00Z");

    private SyncLogRepository syncLog;
    private DeliveryRepository deliveries;
    private TripRunRepository tripRuns;
    private StoreWaitRepository storeWaits;
    private VehicleProblemRepository vehicleProblems;
    private ConflictRepository conflicts;
    private OrderService orders;
    private GoodsReturnRepository goodsReturns;
    private DefaultSyncService service;

    @BeforeEach
    void setUp() {
        syncLog = mock(SyncLogRepository.class);
        deliveries = mock(DeliveryRepository.class);
        tripRuns = mock(TripRunRepository.class);
        storeWaits = mock(StoreWaitRepository.class);
        vehicleProblems = mock(VehicleProblemRepository.class);
        conflicts = mock(ConflictRepository.class);
        orders = mock(OrderService.class);
        goodsReturns = mock(GoodsReturnRepository.class);

        DemoClock clock = mock(DemoClock.class);
        when(clock.now()).thenReturn(NOW);

        CurrentUser user = mock(CurrentUser.class);
        when(user.id()).thenReturn(DRIVER);
        when(user.vehicleId()).thenReturn(Optional.of(VEHICLE));

        ObjectMapper mapper = JsonMapper.builder().build();

        when(deliveries.save(any(Delivery.class))).thenAnswer(call -> call.getArgument(0));
        when(syncLog.save(any(SyncLog.class))).thenAnswer(call -> call.getArgument(0));
        when(tripRuns.save(any(TripRun.class))).thenAnswer(call -> call.getArgument(0));
        when(storeWaits.save(any(StoreWait.class))).thenAnswer(call -> call.getArgument(0));
        when(vehicleProblems.save(any(VehicleProblem.class))).thenAnswer(call -> call.getArgument(0));
        when(conflicts.save(any(Conflict.class))).thenAnswer(call -> call.getArgument(0));
        when(goodsReturns.save(any(GoodsReturn.class))).thenAnswer(call -> call.getArgument(0));

        service = new DefaultSyncService(syncLog, deliveries, tripRuns, storeWaits, vehicleProblems,
                conflicts, goodsReturns, orders, clock, user, mapper);
    }

    /** F1: the same clientId sent twice is applied once. */
    @Test
    void sameClientIdTwiceIsAppliedOnce() {
        SyncRequest request = new SyncRequest(List.of(delivered("c-1", "ord-1", "stp-1", 42)));

        // First attempt - the phone has no record of this clientId yet.
        when(syncLog.existsById("c-1")).thenReturn(false);
        SyncResponse first = service.sync(DRIVER, request);
        assertThat(first.results()).extracting(SyncResponse.Result::result)
                .containsExactly(SyncResult.APPLIED);

        // The phone reconnects before it saw the response and sends the same batch again.
        when(syncLog.existsById("c-1")).thenReturn(true);
        SyncResponse second = service.sync(DRIVER, request);
        assertThat(second.results()).extracting(SyncResponse.Result::result)
                .containsExactly(SyncResult.DUPLICATE);


        // One delivery row, one history row, one order status change - the retry changed nothing.
        verify(deliveries, times(1)).save(any(Delivery.class));
        verify(syncLog, times(1)).save(any(SyncLog.class));
        verify(orders, times(1)).recordOutcome("ord-1", DeliveryOutcome.DELIVERED, 42);
    }

    @Test
    void aDuplicateIsAnsweredWithoutTheServerWritingAnything() {
        when(syncLog.existsById("c-1")).thenReturn(true);

        service.sync(DRIVER, new SyncRequest(List.of(delivered("c-1", "ord-1", "stp-1", 42))));

        verify(deliveries, never()).save(any(Delivery.class));
        verify(syncLog, never()).save(any(SyncLog.class));
        verify(orders, never()).recordOutcome(anyString(), any(), anyInt());
    }

    @Test
    void aReplayLeavesEveryOtherItemInTheBatchAlone() {
        when(syncLog.existsById("c-1")).thenReturn(true);
        when(syncLog.existsById("c-2")).thenReturn(false);

        SyncResponse response = service.sync(DRIVER, new SyncRequest(List.of(
                delivered("c-1", "ord-1", "stp-1", 42),
                delivered("c-2", "ord-2", "stp-2", 30))));

        assertThat(response.results()).extracting(SyncResponse.Result::result)
                .containsExactly(SyncResult.DUPLICATE, SyncResult.APPLIED);
        verify(deliveries, times(1)).save(any(Delivery.class));
        verify(orders, times(1)).recordOutcome("ord-2", DeliveryOutcome.DELIVERED, 30);
    }

    @Test
    void resultsComeBackInTheOrderTheDriverActed() {
        when(syncLog.existsById(anyString())).thenReturn(false);

        SyncResponse response = service.sync(DRIVER, new SyncRequest(List.of(
                delivered("c-1", "ord-1", "stp-1", 42),
                delivered("c-2", "ord-2", "stp-2", 30),
                delivered("c-3", "ord-3", "stp-3", 12))));

        assertThat(response.results()).extracting(SyncResponse.Result::clientId)
                .containsExactly("c-1", "c-2", "c-3");
    }

    @Test
    void aPartialDeliveryBecomesARemainderOrderForWhatStayedOnTheTruck() {
        when(syncLog.existsById(anyString())).thenReturn(false);
        when(orders.get("ord-2")).thenReturn(order("ord-2", 100));

        service.sync(DRIVER, new SyncRequest(List.of(
                partial("c-1", "ord-2", "stp-2", 78, DeliveryReason.DAMAGED))));

        verify(orders).createRemainder("ord-2", 22, "DAMAGED");
    }

    @Test
    void aPartialDeliveryOfEveryCaseNeedsNoRemainder() {
        when(syncLog.existsById(anyString())).thenReturn(false);
        when(orders.get("ord-2")).thenReturn(order("ord-2", 42));

        service.sync(DRIVER, new SyncRequest(List.of(
                partial("c-1", "ord-2", "stp-2", 42, DeliveryReason.STORE_CLOSED))));

        verify(orders, never()).createRemainder(anyString(), anyInt(), anyString());
    }

    /** F6: the driver picks why, so a short delivery without a reason is not accepted. */
    @Test
    void aShortDeliveryWithoutAReasonIsRefused() {
        when(syncLog.existsById(anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.sync(DRIVER, new SyncRequest(List.of(
                partial("c-1", "ord-1", "stp-1", 30, null)))))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("needs a reason");

        verify(deliveries, never()).save(any(Delivery.class));
        verify(orders, never()).recordOutcome(anyString(), any(), anyInt());
    }

    /**
     * F8: the delivery already at the stop was witnessed, so it stands. The clash becomes a conflict
     * row for the dispatcher and the phone is told CONFLICT - not an error, and not a lost record.
     */
    @Test
    void aDeliveryAfterReassignmentLeavesTheWitnessedOneStandingAndRaisesAConflict() {
        when(syncLog.existsById(anyString())).thenReturn(false);
        Delivery earlier = Delivery.record(new Delivery.RecordedDelivery("dlv-earlier", "stp-1",
                "ord-1", "VEH999", DeliveryOutcome.DELIVERED, 20, null, "Nimal", null, null,
                NOW.minusSeconds(120), NOW.minusSeconds(60), "usr-other", "c-0"));
        when(deliveries.findByStopIdAndUndoneAtIsNull("stp-1")).thenReturn(Optional.of(earlier));

        SyncResponse response = service.sync(DRIVER,
                new SyncRequest(List.of(delivered("c-1", "ord-1", "stp-1", 42))));

        assertThat(response.results()).singleElement()
                .satisfies(result -> assertThat(result.result()).isEqualTo(SyncResult.CONFLICT));

        // The witnessed delivery stands and no second one is written over it.
        verify(deliveries, never()).save(any(Delivery.class));
        verify(orders, never()).recordOutcome(anyString(), any(), anyInt());

        ArgumentCaptor<Conflict> raised = ArgumentCaptor.forClass(Conflict.class);
        verify(conflicts).save(raised.capture());
        assertThat(raised.getValue().getDeliveryId()).isEqualTo("dlv-earlier");
        assertThat(raised.getValue().getOrderId()).isEqualTo("ord-1");
        assertThat(raised.getValue().getStopId()).isEqualTo("stp-1");
        assertThat(raised.getValue().getDetails())
                .containsEntry("existingOutcome", "DELIVERED")
                .containsEntry("existingUnits", 20)
                .containsEntry("incomingUnits", 42)
                .containsEntry("incomingDriverId", DRIVER);
        assertThat(raised.getValue().isOpen()).isTrue();
    }

    /** One clash must not cost the driver the rest of the run. */
    @Test
    void aConflictDoesNotStopTheRestOfTheBatch() {
        when(syncLog.existsById(anyString())).thenReturn(false);
        when(deliveries.findByStopIdAndUndoneAtIsNull("stp-1")).thenReturn(Optional.of(
                Delivery.record(new Delivery.RecordedDelivery("dlv-earlier", "stp-1", "ord-1",
                        "VEH999", DeliveryOutcome.DELIVERED, 20, null, "Nimal", null, null,
                        NOW.minusSeconds(120), NOW.minusSeconds(60), "usr-other", "c-0"))));

        SyncResponse response = service.sync(DRIVER, new SyncRequest(List.of(
                delivered("c-1", "ord-1", "stp-1", 42),
                delivered("c-2", "ord-2", "stp-2", 30))));

        assertThat(response.results()).extracting(SyncResponse.Result::result)
                .containsExactly(SyncResult.CONFLICT, SyncResult.APPLIED);
        verify(orders, times(1)).recordOutcome("ord-2", DeliveryOutcome.DELIVERED, 30);
    }

    @Test
    void acceptingATripRecordsTheDriverOnIt() {
        when(syncLog.existsById(anyString())).thenReturn(false);
        when(tripRuns.findById("trp-1")).thenReturn(Optional.empty());

        service.sync(DRIVER, new SyncRequest(List.of(new SyncItem("c-1", SyncActionType.TRIP_ACCEPTED,
                NOW, Map.of("tripId", "trp-1")))));

        verify(tripRuns).save(any(TripRun.class));
        verify(orders, never()).recordOutcome(anyString(), any(), anyInt());
    }

    @Test
    void arrivingMarksTheTripAsUnderWay() {
        when(syncLog.existsById(anyString())).thenReturn(false);
        TripRun run = TripRun.accepted("trp-1", DRIVER, NOW.minusSeconds(600));
        when(tripRuns.findAll()).thenReturn(List.of(run));

        service.sync(DRIVER, new SyncRequest(List.of(new SyncItem("c-1", SyncActionType.ARRIVED, NOW,
                Map.of("stopId", "stp-1", "arrivedAt", NOW.toString())))));

        assertThat(run.getStartedAt()).isEqualTo(NOW);
    }

    /** R4w: the wait is kept with its length worked out, even when the delivery then succeeds. */
    @Test
    void aStoreWaitIsRecordedWithHowLongTheDriverWaited() {
        when(syncLog.existsById(anyString())).thenReturn(false);

        service.sync(DRIVER, new SyncRequest(List.of(new SyncItem("c-1", SyncActionType.STORE_WAIT,
                NOW, Map.of("stopId", "stp-4",
                        "startedAt", NOW.minusSeconds(1_200).toString(),
                        "endedAt", NOW.toString(),
                        "note", "gate was shut")))));

        ArgumentCaptor<StoreWait> saved = ArgumentCaptor.forClass(StoreWait.class);
        verify(storeWaits).save(saved.capture());
        assertThat(saved.getValue().getStopId()).isEqualTo("stp-4");
        assertThat(saved.getValue().getMinutes()).isEqualTo(20);
        assertThat(saved.getValue().getNote()).isEqualTo("gate was shut");
    }

    @Test
    void aVehicleProblemIsReportedAgainstTheDriversOwnVehicle() {
        when(syncLog.existsById(anyString())).thenReturn(false);

        service.sync(DRIVER, new SyncRequest(List.of(new SyncItem("c-1",
                SyncActionType.VEHICLE_PROBLEM, NOW,
                Map.of("tripId", "trp-1", "kind", "FRIDGE_FAULT", "canDrive", true,
                        "fridgeTempC", "11.4", "note", "running warm")))));

        ArgumentCaptor<VehicleProblem> saved = ArgumentCaptor.forClass(VehicleProblem.class);
        verify(vehicleProblems).save(saved.capture());
        VehicleProblem problem = saved.getValue();
        assertThat(problem.getVehicleId()).isEqualTo(VEHICLE);
        assertThat(problem.getTripId()).isEqualTo("trp-1");
        assertThat(problem.getKind()).isEqualTo(VehicleProblemKind.FRIDGE_FAULT);
        assertThat(problem.isCanDrive()).isTrue();
        assertThat(problem.getFridgeTempC()).isEqualByComparingTo("11.4");
        assertThat(problem.isOpen()).isTrue();
    }

    /**
     * US-10.1 - the driver says how many cases are stranded, because that is the number the
     * breakdown re-plan moves (D6b). A report without it still stands; the load is not always there.
     */
    @Test
    void aProblemReportCarriesTheCasesStillOnBoard() {
        when(syncLog.existsById(anyString())).thenReturn(false);

        service.sync(DRIVER, new SyncRequest(List.of(new SyncItem("c-1",
                SyncActionType.VEHICLE_PROBLEM, NOW,
                Map.of("tripId", "trp-1", "kind", "BREAKDOWN", "canDrive", false,
                        "unitsOnBoard", 64, "note", "clutch gone")))));

        ArgumentCaptor<VehicleProblem> saved = ArgumentCaptor.forClass(VehicleProblem.class);
        verify(vehicleProblems).save(saved.capture());
        assertThat(saved.getValue().getUnitsOnBoard()).isEqualTo(64);
        assertThat(saved.getValue().isCanDrive()).isFalse();
    }

    @Test
    void aProblemWithNothingOnBoardSaysSoRatherThanGuessingZero() {
        when(syncLog.existsById(anyString())).thenReturn(false);

        service.sync(DRIVER, new SyncRequest(List.of(new SyncItem("c-1",
                SyncActionType.VEHICLE_PROBLEM, NOW,
                Map.of("kind", "TYRE", "canDrive", true, "note", "slow leak")))));

        ArgumentCaptor<VehicleProblem> saved = ArgumentCaptor.forClass(VehicleProblem.class);
        verify(vehicleProblems).save(saved.capture());
        // Zero cases and "nobody said" are different answers to the re-plan.
        assertThat(saved.getValue().getUnitsOnBoard()).isNull();
    }

    /** R8r — goods handed back at the depot are recorded against the trip and the driver. */
    @Test
    void goodsHandedBackAtTheDepotAreRecorded() {
        when(syncLog.existsById(anyString())).thenReturn(false);

        service.sync(DRIVER, new SyncRequest(List.of(new SyncItem("c-1",
                SyncActionType.GOODS_RETURNED, NOW,
                Map.of("tripId", "trp-1", "orderId", "ord-1", "units", 12,
                        "reason", DeliveryReason.STORE_CLOSED.name())))));

        ArgumentCaptor<GoodsReturn> saved = ArgumentCaptor.forClass(GoodsReturn.class);
        verify(goodsReturns).save(saved.capture());
        GoodsReturn returned = saved.getValue();
        assertThat(returned.getTripId()).isEqualTo("trp-1");
        assertThat(returned.getOrderId()).isEqualTo("ord-1");
        assertThat(returned.getUnits()).isEqualTo(12);
        assertThat(returned.getReason()).isEqualTo(DeliveryReason.STORE_CLOSED);
        assertThat(returned.getRecordedBy()).isEqualTo(DRIVER);
    }

    /** A handback is stock coming back, not an outcome on the order - the shelf still owes for it. */
    @Test
    void handingGoodsBackDoesNotSettleTheOrder() {
        when(syncLog.existsById(anyString())).thenReturn(false);

        service.sync(DRIVER, new SyncRequest(List.of(new SyncItem("c-1",
                SyncActionType.GOODS_RETURNED, NOW,
                Map.of("tripId", "trp-1", "orderId", "ord-1", "units", 12,
                        "reason", DeliveryReason.STORE_CLOSED.name())))));

        verify(orders, never()).recordOutcome(anyString(), any(), anyInt());
        verify(orders, never()).createRemainder(anyString(), anyInt(), anyString());
    }

    /**
     * A handback sent twice after a dropped connection is still one handback - the same guarantee
     * every other action gets from the clientId, and the reason a driver can retry without fear.
     */
    @Test
    void aHandbackRetriedAfterADroppedConnectionIsStillRecordedOnce() {
        SyncRequest request = new SyncRequest(List.of(new SyncItem("c-1",
                SyncActionType.GOODS_RETURNED, NOW,
                Map.of("tripId", "trp-1", "orderId", "ord-1", "units", 12,
                        "reason", DeliveryReason.STORE_CLOSED.name()))));

        when(syncLog.existsById("c-1")).thenReturn(false);
        assertThat(service.sync(DRIVER, request).results()).extracting(SyncResponse.Result::result)
                .containsExactly(SyncResult.APPLIED);

        when(syncLog.existsById("c-1")).thenReturn(true);
        assertThat(service.sync(DRIVER, request).results()).extracting(SyncResponse.Result::result)
                .containsExactly(SyncResult.DUPLICATE);

        verify(goodsReturns, times(1)).save(any(GoodsReturn.class));
    }

    /** An unreadable payload is the phone's problem, so it is a 400 and not a retryable 500. */
    @Test
    void anUnreadablePayloadIsRefusedAsBadRequest() {
        when(syncLog.existsById(anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.sync(DRIVER, new SyncRequest(List.of(new SyncItem(
                "c-1", SyncActionType.VEHICLE_PROBLEM, NOW, Map.of("kind", "NOT_A_KIND"))))))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("cannot read a VEHICLE_PROBLEM payload");
    }

    @Test
    void aSyncedBatchStampsTheDriversTripSoTheBoardCanTellItFromAGap() {
        when(syncLog.existsById(anyString())).thenReturn(false);
        TripRun mine = TripRun.accepted("trp-1", DRIVER, NOW.minusSeconds(600));
        TripRun someoneElse = TripRun.accepted("trp-2", "usr-other", NOW.minusSeconds(600));
        when(tripRuns.findAll()).thenReturn(List.of(mine, someoneElse));

        service.sync(DRIVER, new SyncRequest(List.of(delivered("c-1", "ord-1", "stp-1", 42))));

        assertThat(mine.getLastSyncAt()).isEqualTo(NOW);
        assertThat(someoneElse.getLastSyncAt()).isNull();
    }

    private SyncItem delivered(String clientId, String orderId, String stopId, int units) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("stopId", stopId);
        payload.put("orderId", orderId);
        payload.put("outcome", DeliveryOutcome.DELIVERED.name());
        payload.put("units", units);
        return new SyncItem(clientId, SyncActionType.DELIVERY_RECORDED, NOW, payload);
    }

    private SyncItem partial(String clientId, String orderId, String stopId, int units,
            DeliveryReason reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("stopId", stopId);
        payload.put("orderId", orderId);
        payload.put("outcome", DeliveryOutcome.PARTIAL.name());
        payload.put("units", units);
        if (reason != null) {
            payload.put("reason", reason.name());
        }
        return new SyncItem(clientId, SyncActionType.DELIVERY_RECORDED, NOW, payload);
    }

    private OrderDto order(String id, int units) {
        return new OrderDto(id, "S1-001", "OUT003", "OUT003 - Colombo", "Fresh",
                TemperatureRequirement.CHILLED, units, BigDecimal.TEN, BigDecimal.ONE,
                LocalDate.parse("2026-10-01"), OrderStatus.ON_THE_WAY, OrderSource.SEED, false, 1,
                false, null, true, NOW.atOffset(ZoneOffset.UTC), NOW.atOffset(ZoneOffset.UTC));
    }
}