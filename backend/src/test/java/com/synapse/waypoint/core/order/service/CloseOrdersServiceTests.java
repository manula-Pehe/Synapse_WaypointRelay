package com.synapse.waypoint.core.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.core.order.dto.CloseResultDto;
import com.synapse.waypoint.core.order.dto.CloseStatusDto;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderEventDto;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.order.support.SignedInUser;

/** Closing orders for a run and depot. Data is invented. */
@SpringBootTest
@Transactional
class CloseOrdersServiceTests {

    private static final String OUTLET = "OUT991";
    private static final String OTHER_DEPOT_OUTLET = "OUT993";
    private static final String OTHER_DEPOT = "Otherdepot";
    private static final String DISPATCHER = "usr-t-dispatch";
    private static final LocalDate RUN_DATE = OrderFixtures.RUN_DATE;

    @Autowired CloseOrdersService closeOrders;
    @Autowired OrderService orderService;
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void createOutletsAndDispatcher() {
        OrderFixtures.insertOutlet(jdbc, OUTLET, "Fresh", "Testdistrict");
        OrderFixtures.insertOutlet(jdbc, OTHER_DEPOT_OUTLET, "Fresh", "Otherdistrict", OTHER_DEPOT);
        OrderFixtures.insertUser(jdbc, DISPATCHER, "DISPATCHER", null);
    }

    @AfterEach
    void signOut() {
        SignedInUser.signOut();
    }

    @Test
    void shouldAutoConfirmFreshAmbientOrdersAndLeaveTheRestUnconfirmed() {
        Order freshAmbient = prepared("Fresh", TemperatureRequirement.AMBIENT);
        Order freshChilled = prepared("Fresh", TemperatureRequirement.CHILLED);
        Order style = prepared("Style", TemperatureRequirement.AMBIENT);
        Order tech = prepared("Tech", TemperatureRequirement.AMBIENT);

        closeOrders.close(RUN_DATE, OrderFixtures.DEPOT);

        assertThat(statusOf(freshAmbient)).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orderService.get(freshAmbient.getId()).autoConfirm()).isTrue();
        assertThat(statusOf(freshChilled)).isEqualTo(OrderStatus.PREPARED);
        assertThat(statusOf(style)).isEqualTo(OrderStatus.PREPARED);
        assertThat(statusOf(tech)).isEqualTo(OrderStatus.PREPARED);
    }

    @Test
    void shouldCountAlreadyConfirmedAutoConfirmedAndNotConfirmedOrders() {
        OrderFixtures.save(orders, OUTLET, OrderStatus.CONFIRMED);
        OrderFixtures.save(orders, OUTLET, OrderStatus.CONFIRMED);
        prepared("Fresh", TemperatureRequirement.AMBIENT);
        prepared("Fresh", TemperatureRequirement.CHILLED);
        prepared("Style", TemperatureRequirement.AMBIENT);
        OrderFixtures.save(orders, OUTLET, OrderStatus.CANCELLED);

        CloseResultDto result = closeOrders.close(RUN_DATE, OrderFixtures.DEPOT);

        assertThat(result.confirmed()).isEqualTo(2);
        assertThat(result.autoConfirmed()).isEqualTo(1);
        assertThat(result.notConfirmed()).isEqualTo(2);
        assertThat(result.closedAt()).isNotNull();
    }

    @Test
    void shouldOnlyTouchOrdersOfTheRunAndDepotBeingClosed() {
        Order otherDepot = OrderFixtures.save(orders, OTHER_DEPOT_OUTLET, OrderStatus.PREPARED);
        Order otherDate = orders.saveAndFlush(Order.create(OrderFixtures.newOrder(OUTLET, OrderStatus.PREPARED,
                TemperatureRequirement.AMBIENT, 10, OrderSource.SEED,
                RUN_DATE.plusDays(1)), OrderFixtures.CREATED_AT));

        CloseResultDto result = closeOrders.close(RUN_DATE, OrderFixtures.DEPOT);

        assertThat(result.autoConfirmed()).isZero();
        assertThat(statusOf(otherDepot)).isEqualTo(OrderStatus.PREPARED);
        assertThat(statusOf(otherDate)).isEqualTo(OrderStatus.PREPARED);
        assertThat(orderService.isClosed(RUN_DATE, OTHER_DEPOT)).isFalse();
        assertThat(orderService.isClosed(RUN_DATE.plusDays(1), OrderFixtures.DEPOT)).isFalse();
    }

    @Test
    void shouldWriteAHistoryRowForEveryAutoConfirmedOrder() {
        Order order = prepared("Fresh", TemperatureRequirement.AMBIENT);

        closeOrders.close(RUN_DATE, OrderFixtures.DEPOT);

        OrderEventDto event = orderService.history(order.getId()).get(0);
        assertThat(event.toStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(event.details()).containsEntry("auto", true);
    }

    @Test
    void shouldRefuseASecondClose() {
        closeOrders.close(RUN_DATE, OrderFixtures.DEPOT);

        assertThatThrownBy(() -> closeOrders.close(RUN_DATE, OrderFixtures.DEPOT))
                .isInstanceOfSatisfying(DomainException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.ORDERS_CLOSED));
    }

    @Test
    void shouldTreatDepotNamesIgnoringCase() {
        closeOrders.close(RUN_DATE, OrderFixtures.DEPOT.toUpperCase());

        assertThat(orderService.isClosed(RUN_DATE, OrderFixtures.DEPOT)).isTrue();
        assertThatThrownBy(() -> closeOrders.close(RUN_DATE, OrderFixtures.DEPOT.toLowerCase()))
                .isInstanceOfSatisfying(DomainException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.ORDERS_CLOSED));
    }

    @Test
    void shouldRefuseAnUnknownDepot() {
        assertThatThrownBy(() -> closeOrders.close(RUN_DATE, "Nowhere"))
                .isInstanceOfSatisfying(DomainException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION));
    }

    @Test
    void shouldRecordTheSignedInDispatcherAsWhoClosedTheOrders() {
        SignedInUser.asDispatcher(DISPATCHER);

        closeOrders.close(RUN_DATE, OrderFixtures.DEPOT);

        assertThat(closeOrders.status(RUN_DATE, OrderFixtures.DEPOT).closedBy()).isEqualTo(DISPATCHER);
    }

    @Test
    void shouldCloseAsTheSystemWhenNobodyIsSignedIn() {
        Order order = prepared("Fresh", TemperatureRequirement.AMBIENT);

        CloseResultDto result = closeOrders.close(RUN_DATE, OrderFixtures.DEPOT);

        assertThat(result.autoConfirmed()).isEqualTo(1);
        CloseStatusDto status = closeOrders.status(RUN_DATE, OrderFixtures.DEPOT);
        assertThat(status.closed()).isTrue();
        assertThat(status.closedBy()).isNull();
        assertThat(orderService.history(order.getId()).get(0).actor()).isNull();
    }

    @Test
    void shouldReportIsClosedOnlyAfterClosing() {
        assertThat(orderService.isClosed(RUN_DATE, OrderFixtures.DEPOT)).isFalse();

        closeOrders.close(RUN_DATE, OrderFixtures.DEPOT);

        assertThat(orderService.isClosed(RUN_DATE, OrderFixtures.DEPOT)).isTrue();
    }

    @Test
    void shouldReportOpenStatusWithTheCutOffFourPmTheDayBefore() {
        CloseStatusDto status = closeOrders.status(RUN_DATE, OrderFixtures.DEPOT);

        assertThat(status.closed()).isFalse();
        assertThat(status.closedAt()).isNull();
        assertThat(status.closedBy()).isNull();
        assertThat(status.cutOffAt()).isEqualTo(OffsetDateTime.parse("2026-09-30T16:00:00+05:30"));
    }

    @Test
    void shouldReportClosedStatusWithTheTimeItWasClosed() {
        CloseResultDto result = closeOrders.close(RUN_DATE, OrderFixtures.DEPOT);

        CloseStatusDto status = closeOrders.status(RUN_DATE, OrderFixtures.DEPOT);

        assertThat(status.closed()).isTrue();
        assertThat(status.closedAt()).isEqualTo(result.closedAt());
        assertThat(status.cutOffAt()).isEqualTo(OffsetDateTime.parse("2026-09-30T16:00:00+05:30"));
    }

    private Order prepared(String brand, TemperatureRequirement temp) {
        return OrderFixtures.save(orders, OUTLET, brand, temp, OrderStatus.PREPARED);
    }

    private OrderStatus statusOf(Order order) {
        OrderDto dto = orderService.get(order.getId());
        return dto.status();
    }
}
