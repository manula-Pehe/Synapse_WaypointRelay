package com.synapse.waypoint.core.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.dto.CreateOrderRequest;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.event.OrderStatusChanged;
import com.synapse.waypoint.core.order.exception.InvalidStatusException;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.order.support.SignedInUser;

/** One OrderStatusChanged per history row, so notifications (F10) can listen later. */
@SpringBootTest
@Transactional
@RecordApplicationEvents
class OrderStatusChangedEventTests {

    private static final String OUTLET = "OUT991";
    private static final String DISPATCHER = "usr-t-dispatch";

    @Autowired OrderService service;
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;
    @Autowired ApplicationEvents events;

    @BeforeEach
    void createOutletAndUser() {
        OrderFixtures.insertOutlet(jdbc, OUTLET, "Fresh", "Testdistrict");
        OrderFixtures.insertUser(jdbc, DISPATCHER, "DISPATCHER", null);
    }

    @AfterEach
    void signOut() {
        SignedInUser.signOut();
    }

    @Test
    void shouldPublishOneEventWithTheChangeAndItsActor() {
        Order order = OrderFixtures.save(orders, OUTLET, OrderStatus.PREPARED);
        SignedInUser.asDispatcher(DISPATCHER);

        service.confirm(order.getId());

        assertThat(events.stream(OrderStatusChanged.class)).singleElement().satisfies(event -> {
            assertThat(event.orderId()).isEqualTo(order.getId());
            assertThat(event.outletId()).isEqualTo(OUTLET);
            assertThat(event.type()).isEqualTo("CONFIRMED");
            assertThat(event.from()).isEqualTo(OrderStatus.PREPARED);
            assertThat(event.to()).isEqualTo(OrderStatus.CONFIRMED);
            assertThat(event.actorUserId()).isEqualTo(DISPATCHER);
            assertThat(event.at()).isNotNull();
        });
    }

    @Test
    void shouldPublishAnEditEventWithoutAStatusChange() {
        Order order = OrderFixtures.save(orders, OUTLET, OrderStatus.PREPARED);

        service.editUnits(order.getId(), 5);

        assertThat(events.stream(OrderStatusChanged.class)).singleElement().satisfies(event -> {
            assertThat(event.type()).isEqualTo("EDITED");
            assertThat(event.from()).isEqualTo(OrderStatus.PREPARED);
            assertThat(event.to()).isEqualTo(OrderStatus.PREPARED);
            assertThat(event.actorUserId()).isNull();
        });
    }

    @Test
    void shouldPublishAnEventForANewOrderWithNoPreviousStatus() {
        OrderDto created = service.createPhoneInOrder(
                new CreateOrderRequest(OUTLET, OrderFixtures.RUN_DATE, TemperatureRequirement.AMBIENT, 3, null));

        assertThat(events.stream(OrderStatusChanged.class)).singleElement().satisfies(event -> {
            assertThat(event.orderId()).isEqualTo(created.id());
            assertThat(event.from()).isNull();
            assertThat(event.to()).isEqualTo(OrderStatus.CONFIRMED);
        });
    }

    @Test
    void shouldPublishNothingWhenTheChangeIsRefused() {
        Order order = OrderFixtures.save(orders, OUTLET, OrderStatus.PREPARED);

        assertThatThrownBy(() -> service.markLoaded(order.getId())).isInstanceOf(InvalidStatusException.class);

        assertThat(events.stream(OrderStatusChanged.class)).isEmpty();
    }
}
