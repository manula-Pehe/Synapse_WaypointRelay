package com.synapse.waypoint.core.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderEventDto;
import com.synapse.waypoint.core.order.entity.DeliveryOutcome;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.order.support.SignedInUser;

import static com.synapse.waypoint.core.order.entity.OrderStatus.*;

/** Status changes through OrderService: lifecycle, history rows and store scope. Data is invented. */
@SpringBootTest
@Transactional
class DefaultOrderServiceTests {

    private static final String OUTLET = "OUT991";
    private static final String OTHER_OUTLET = "OUT992";
    private static final String STORE_USER = "usr-t-store";
    private static final LocalDate NEXT_DAY = LocalDate.parse("2026-10-02");

    private interface Operation extends BiFunction<OrderService, String, OrderDto> {
    }

    @Autowired OrderService service;
    @Autowired OrderRepository orders;
    @Autowired DemoClock clock;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void createOutletsAndStoreUser() {
        OrderFixtures.insertOutlet(jdbc, OUTLET, "Fresh", "Testdistrict");
        OrderFixtures.insertOutlet(jdbc, OTHER_OUTLET, "Fresh", "Otherdistrict");
        OrderFixtures.insertUser(jdbc, STORE_USER, "STORE_MANAGER", OUTLET);
    }

    @AfterEach
    void signOut() {
        SignedInUser.signOut();
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("allowedChanges")
    void shouldApplyEveryAllowedTransitionAndWriteOneHistoryRow(OrderStatus from, OrderStatus to, Operation change) {
        Order order = OrderFixtures.save(orders, OUTLET, from);
        Instant before = clock.now();

        OrderDto result = change.apply(service, order.getId());

        assertThat(result.status()).isEqualTo(to);
        List<OrderEventDto> history = service.history(order.getId());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).fromStatus()).isEqualTo(from);
        assertThat(history.get(0).toStatus()).isEqualTo(to);
        assertThat(history.get(0).type()).isEqualTo(to.name());
        assertThat(history.get(0).at()).isBetween(before, clock.now());
    }

    @ParameterizedTest(name = "{0} cannot be {1}")
    @MethodSource("forbiddenChanges")
    void shouldRefuseForbiddenChangesWithoutTouchingTheOrderOrItsHistory(OrderStatus from, OrderStatus attempted,
            Operation change) {
        Order order = OrderFixtures.save(orders, OUTLET, from);

        assertThatThrownBy(() -> change.apply(service, order.getId()))
                .isInstanceOfSatisfying(DomainException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_STATUS));

        assertThat(service.get(order.getId()).status()).isEqualTo(from);
        assertThat(service.history(order.getId())).isEmpty();
    }

    @Test
    void shouldRecordSystemChangesWithoutAnActor() {
        Order order = OrderFixtures.save(orders, OUTLET, PREPARED);

        service.confirm(order.getId());

        assertThat(service.history(order.getId()).get(0).actor()).isNull();
    }

    @Test
    void shouldAutoConfirmAPreparedOrderWithoutAUserAndMarkIt() {
        Order order = OrderFixtures.save(orders, OUTLET, PREPARED);

        OrderDto result = service.autoConfirm(order.getId());

        assertThat(result.status()).isEqualTo(CONFIRMED);
        assertThat(result.autoConfirm()).isTrue();
        assertThat(result.confirmedAt()).isNotNull();
        OrderEventDto event = service.history(order.getId()).get(0);
        assertThat(event.type()).isEqualTo("CONFIRMED");
        assertThat(event.actor()).isNull();
        assertThat(event.details()).containsEntry("auto", true);
    }

    @Test
    void shouldRefuseToAutoConfirmAnOrderThatIsNotPrepared() {
        Order order = OrderFixtures.save(orders, OUTLET, CONFIRMED);

        assertThatThrownBy(() -> service.autoConfirm(order.getId()))
                .isInstanceOfSatisfying(DomainException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_STATUS));
    }

    @Test
    void shouldRecordTheSignedInUserAsActorAndConfirmedBy() {
        Order order = OrderFixtures.save(orders, OUTLET, PREPARED);
        SignedInUser.asStoreManager(STORE_USER, OUTLET);

        service.confirm(order.getId());

        assertThat(service.history(order.getId()).get(0).actor()).isEqualTo(STORE_USER);
        assertThat(orders.findById(order.getId()).orElseThrow().getConfirmedBy()).isEqualTo(STORE_USER);
    }

    @Test
    void shouldKeepReasonAndNewDateInTheHistoryWhenMoved() {
        Order order = OrderFixtures.save(orders, OUTLET, CONFIRMED);

        OrderDto moved = service.markMoved(order.getId(), NEXT_DAY, "No vehicle fits");

        assertThat(moved.runDate()).isEqualTo(NEXT_DAY);
        assertThat(service.history(order.getId()).get(0).details())
                .containsEntry("reason", "No vehicle fits")
                .containsEntry("fromDate", "2026-10-01")
                .containsEntry("toDate", "2026-10-02");
    }

    @Test
    void shouldRefuseMovingToADateThatIsNotLater() {
        Order order = OrderFixtures.save(orders, OUTLET, CONFIRMED);

        assertThatThrownBy(() -> service.markMoved(order.getId(), OrderFixtures.RUN_DATE, "x"))
                .isInstanceOfSatisfying(DomainException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION));
    }

    @Test
    void shouldKeepThePlanInTheHistoryWhenPlanned() {
        Order order = OrderFixtures.save(orders, OUTLET, CONFIRMED);

        service.markPlanned(order.getId(), "plan-7");

        assertThat(service.history(order.getId()).get(0).details()).containsEntry("planId", "plan-7");
    }

    @Test
    void shouldChangeUnitsOfAPreparedOrderAndRecordAnEditWithoutAStatusChange() {
        Order order = OrderFixtures.save(orders, OUTLET, PREPARED);

        OrderDto edited = service.editUnits(order.getId(), 4);

        assertThat(edited.units()).isEqualTo(4);
        assertThat(edited.status()).isEqualTo(PREPARED);
        OrderEventDto event = service.history(order.getId()).get(0);
        assertThat(event.type()).isEqualTo("EDITED");
        assertThat(event.fromStatus()).isEqualTo(PREPARED);
        assertThat(event.toStatus()).isEqualTo(PREPARED);
        assertThat(event.details()).containsEntry("fromUnits", 10).containsEntry("toUnits", 4);
    }

    @Test
    void shouldRefuseEditingUnitsAfterConfirmation() {
        Order order = OrderFixtures.save(orders, OUTLET, CONFIRMED);

        assertThatThrownBy(() -> service.editUnits(order.getId(), 4))
                .isInstanceOfSatisfying(DomainException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.INVALID_STATUS));
    }

    @Test
    void shouldRefuseDeliveredUnitsAboveTheOrderedUnits() {
        Order order = OrderFixtures.save(orders, OUTLET, ON_THE_WAY);

        assertThatThrownBy(() -> service.recordOutcome(order.getId(), DeliveryOutcome.PARTIAL, 11))
                .isInstanceOfSatisfying(DomainException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION));
    }

    @Test
    void shouldHideAnotherOutletsOrderFromAStoreManager() {
        Order foreign = OrderFixtures.save(orders, OTHER_OUTLET, PREPARED);
        SignedInUser.asStoreManager(STORE_USER, OUTLET);

        assertThatThrownBy(() -> service.get(foreign.getId())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.confirm(foreign.getId())).isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.history(foreign.getId())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldLetADispatcherSeeAnyOutletsOrder() {
        Order order = OrderFixtures.save(orders, OTHER_OUTLET, PREPARED);
        SignedInUser.asDispatcher("usr-t-dispatch");

        assertThat(service.get(order.getId()).outletId()).isEqualTo(OTHER_OUTLET);
    }

    @Test
    void shouldReturnNotFoundForAnUnknownOrder() {
        assertThatThrownBy(() -> service.get("missing")).isInstanceOf(NotFoundException.class);
    }

    static Stream<Arguments> allowedChanges() {
        return Stream.of(
                change(PREPARED, CONFIRMED, (s, id) -> s.confirm(id)),
                change(PREPARED, CANCELLED, (s, id) -> s.cancel(id, "Not needed")),
                change(CONFIRMED, PLANNED, (s, id) -> s.markPlanned(id, "plan-1")),
                change(CONFIRMED, MOVED, (s, id) -> s.markMoved(id, NEXT_DAY, "Deferred")),
                change(CONFIRMED, CANCELLED, (s, id) -> s.cancel(id, "Not needed")),
                change(PLANNED, LOADED, (s, id) -> s.markLoaded(id)),
                change(PLANNED, MOVED, (s, id) -> s.markMoved(id, NEXT_DAY, "Breakdown")),
                change(LOADED, ON_THE_WAY, (s, id) -> s.markOnTheWay(id)),
                change(ON_THE_WAY, DELIVERED, (s, id) -> s.recordOutcome(id, DeliveryOutcome.DELIVERED, 10)),
                change(ON_THE_WAY, PARTIAL, (s, id) -> s.recordOutcome(id, DeliveryOutcome.PARTIAL, 6)),
                change(ON_THE_WAY, FAILED, (s, id) -> s.recordOutcome(id, DeliveryOutcome.FAILED, 0)),
                change(MOVED, CONFIRMED, (s, id) -> s.confirm(id)),
                change(MOVED, CANCELLED, (s, id) -> s.cancel(id, "Not needed")),
                change(FAILED, MOVED, (s, id) -> s.markMoved(id, NEXT_DAY, "Store closed")),
                change(FAILED, CANCELLED, (s, id) -> s.cancel(id, "Not needed")));
    }

    static Stream<Arguments> forbiddenChanges() {
        return Stream.of(
                change(PREPARED, PLANNED, (s, id) -> s.markPlanned(id, "plan-1")),
                change(PREPARED, LOADED, (s, id) -> s.markLoaded(id)),
                change(CONFIRMED, LOADED, (s, id) -> s.markLoaded(id)),
                change(CONFIRMED, CONFIRMED, (s, id) -> s.confirm(id)),
                change(PLANNED, CANCELLED, (s, id) -> s.cancel(id, "x")),
                change(LOADED, DELIVERED, (s, id) -> s.recordOutcome(id, DeliveryOutcome.DELIVERED, 10)),
                change(DELIVERED, CANCELLED, (s, id) -> s.cancel(id, "x")),
                change(DELIVERED, MOVED, (s, id) -> s.markMoved(id, NEXT_DAY, "x")),
                change(CANCELLED, CONFIRMED, (s, id) -> s.confirm(id)),
                change(ON_THE_WAY, CANCELLED, (s, id) -> s.cancel(id, "x")),
                change(PARTIAL, ON_THE_WAY, (s, id) -> s.markOnTheWay(id)));
    }

    private static Arguments change(OrderStatus from, OrderStatus to, Operation operation) {
        return Arguments.of(from, to, operation);
    }
}
