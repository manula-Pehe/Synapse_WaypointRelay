package com.synapse.waypoint.core.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.error.NotFoundException;
import com.synapse.waypoint.core.order.dto.CreateOrderRequest;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderEventDto;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.order.support.SignedInUser;

/** Store, phone-in and remainder orders. Data is invented. */
@SpringBootTest
@Transactional
class OrderCreationTests {

    private static final String OUTLET = "OUT991";
    private static final String OTHER_OUTLET = "OUT992";
    private static final String STORE_USER = "usr-t-store";
    private static final String DISPATCHER = "usr-t-dispatch";

    @Autowired OrderService service;
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void createOutletsAndUsers() {
        OrderFixtures.insertOutlet(jdbc, OUTLET, "Fresh", "Testdistrict");
        OrderFixtures.insertOutlet(jdbc, OTHER_OUTLET, "Style", "Otherdistrict");
        OrderFixtures.insertUser(jdbc, STORE_USER, "STORE_MANAGER", OUTLET);
        OrderFixtures.insertUser(jdbc, DISPATCHER, "DISPATCHER", null);
    }

    @AfterEach
    void signOut() {
        SignedInUser.signOut();
    }

    @Test
    void shouldCreateAStoreOrderAsPreparedAndCheckedByTheStore() {
        SignedInUser.asStoreManager(STORE_USER, OUTLET);

        OrderDto order = service.createStoreOrder(request(OUTLET, 12, "ignored by store"));

        assertThat(order.status()).isEqualTo(OrderStatus.PREPARED);
        assertThat(order.source()).isEqualTo(OrderSource.STORE);
        assertThat(order.storeChecked()).isTrue();
        assertThat(order.brand()).isEqualTo("Fresh");
        assertThat(order.outletName()).isEqualTo("OUT991 · Testdistrict");
        assertThat(order.weightKg()).isPositive();
        assertThat(order.volumeM3()).isPositive();
    }

    @Test
    void shouldWriteTheCreationAsTheFirstHistoryRow() {
        SignedInUser.asStoreManager(STORE_USER, OUTLET);

        OrderDto order = service.createStoreOrder(request(OUTLET, 12, "extra for the weekend"));

        OrderEventDto event = service.history(order.id()).get(0);
        assertThat(service.history(order.id())).hasSize(1);
        assertThat(event.type()).isEqualTo("PREPARED");
        assertThat(event.fromStatus()).isNull();
        assertThat(event.toStatus()).isEqualTo(OrderStatus.PREPARED);
        assertThat(event.actor()).isEqualTo(STORE_USER);
        assertThat(event.details()).containsEntry("source", "STORE").containsEntry("note", "extra for the weekend");
    }

    @Test
    void shouldNotLetAStoreManagerOrderForAnotherOutlet() {
        SignedInUser.asStoreManager(STORE_USER, OUTLET);

        assertThatThrownBy(() -> service.createStoreOrder(request(OTHER_OUTLET, 5, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldCreateAPhoneInOrderAsConfirmedAndNotYetCheckedByTheStore() {
        SignedInUser.asDispatcher(DISPATCHER);

        OrderDto order = service.createPhoneInOrder(request(OTHER_OUTLET, 8, "Phoned at 3 PM"));

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.source()).isEqualTo(OrderSource.PHONE_IN);
        assertThat(order.storeChecked()).isFalse();
        assertThat(order.brand()).isEqualTo("Style");
        OrderEventDto event = service.history(order.id()).get(0);
        assertThat(event.actor()).isEqualTo(DISPATCHER);
        assertThat(event.details()).containsEntry("source", "PHONE_IN").containsEntry("note", "Phoned at 3 PM");
    }

    @Test
    void shouldRefuseAnOrderForAnUnknownOutlet() {
        assertThatThrownBy(() -> service.createPhoneInOrder(request("OUT000", 5, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldRefuseAnOrderWithoutUnits() {
        assertThatThrownBy(() -> service.createPhoneInOrder(request(OUTLET, 0, null)))
                .isInstanceOfSatisfying(DomainException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION));
    }

    @Test
    void shouldCreateARemainderLinkedToItsParentWithRefPlusR() {
        Order parent = OrderFixtures.save(orders, OUTLET, OrderStatus.LOADED);

        OrderDto remainder = service.createRemainder(parent.getId(), 4, "Short at loading");

        assertThat(remainder.ref()).isEqualTo(parent.getRef() + "-R");
        assertThat(remainder.source()).isEqualTo(OrderSource.REMAINDER);
        assertThat(remainder.parentOrderId()).isEqualTo(parent.getId());
        assertThat(remainder.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(remainder.outletId()).isEqualTo(OUTLET);
        assertThat(remainder.runDate()).isEqualTo(parent.getRunDate());
        assertThat(remainder.units()).isEqualTo(4);
        assertThat(remainder.weightKg()).isEqualByComparingTo("40.00");
        assertThat(service.history(remainder.id()).get(0).details())
                .containsEntry("reason", "Short at loading")
                .containsEntry("parentOrderId", parent.getId());
    }

    @Test
    void shouldNumberFurtherRemaindersOfTheSameOrder() {
        Order parent = OrderFixtures.save(orders, OUTLET, OrderStatus.LOADED);

        OrderDto first = service.createRemainder(parent.getId(), 1, null);
        OrderDto second = service.createRemainder(parent.getId(), 1, null);
        OrderDto third = service.createRemainder(parent.getId(), 1, null);

        assertThat(first.ref()).isEqualTo(parent.getRef() + "-R");
        assertThat(second.ref()).isEqualTo(parent.getRef() + "-R2");
        assertThat(third.ref()).isEqualTo(parent.getRef() + "-R3");
    }

    @Test
    void shouldRefuseARemainderLargerThanTheParent() {
        Order parent = OrderFixtures.save(orders, OUTLET, OrderStatus.LOADED);

        assertThatThrownBy(() -> service.createRemainder(parent.getId(), 11, null))
                .isInstanceOfSatisfying(DomainException.class,
                        e -> assertThat(e.code()).isEqualTo(ErrorCode.VALIDATION));
    }

    @Test
    void shouldNotLetAStoreManagerCreateARemainderOfAnotherOutletsOrder() {
        Order foreign = OrderFixtures.save(orders, OTHER_OUTLET, OrderStatus.LOADED);
        SignedInUser.asStoreManager(STORE_USER, OUTLET);

        assertThatThrownBy(() -> service.createRemainder(foreign.getId(), 1, null))
                .isInstanceOf(NotFoundException.class);
    }

    private static CreateOrderRequest request(String outletId, int units, String note) {
        return new CreateOrderRequest(outletId, OrderFixtures.RUN_DATE, TemperatureRequirement.AMBIENT, units, note);
    }
}
