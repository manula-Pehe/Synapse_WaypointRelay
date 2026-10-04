package com.synapse.waypoint.core.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.core.order.dto.OrderDto;
import com.synapse.waypoint.core.order.dto.OrderEventDto;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.support.OrderFixtures;
import com.synapse.waypoint.core.order.support.SignedInUser;

/** What a store may do to a deferred (MOVED) order after the cut-off. Data is invented. */
@SpringBootTest
@Transactional
class MovedOrderChangesTests {

    private static final String OUTLET = "OUT991";
    private static final String STORE_USER = "usr-t-store";
    private static final String CLOSED_DEPOT = OrderFixtures.DEPOT;

    @Autowired OrderService service;
    @Autowired CloseOrdersService closeOrders;
    @Autowired OrderRepository orders;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void createOutletAndStoreUser() {
        OrderFixtures.insertOutlet(jdbc, OUTLET, "Fresh", "Testdistrict");
        OrderFixtures.insertUser(jdbc, STORE_USER, "STORE_MANAGER", OUTLET);
        SignedInUser.asStoreManager(STORE_USER, OUTLET);
    }

    @AfterEach
    void signOut() {
        SignedInUser.signOut();
    }

    @Test
    void shouldScaleWeightAndVolumeAndWriteAnEditedRowWhenAMovedOrderIsResized() {
        Order order = OrderFixtures.save(orders, OUTLET, OrderStatus.MOVED);

        OrderDto resized = service.resizeMoved(order.getId(), 4);

        assertThat(resized.units()).isEqualTo(4);
        assertThat(resized.status()).isEqualTo(OrderStatus.MOVED);
        assertThat(resized.weightKg()).isEqualByComparingTo(new BigDecimal("40.00"));
        assertThat(resized.volumeM3()).isEqualByComparingTo(new BigDecimal("0.400"));
        OrderEventDto event = lastEvent(order.getId());
        assertThat(event.type()).isEqualTo("EDITED");
        assertThat(event.details()).containsEntry("fromUnits", 10).containsEntry("toUnits", 4);
    }

    @Test
    void shouldResizeAMovedOrderOfAClosedRun() {
        Order order = OrderFixtures.save(orders, OUTLET, OrderStatus.MOVED);
        closeRunOf(order);

        assertThat(service.resizeMoved(order.getId(), 9).units()).isEqualTo(9);
    }

    @Test
    void shouldRejectAResizeOutsideOneToLessThanTheCurrentUnits() {
        Order order = OrderFixtures.save(orders, OUTLET, OrderStatus.MOVED);

        for (int units : new int[] { 0, -3, 10, 11 }) {
            assertThatThrownBy(() -> service.resizeMoved(order.getId(), units))
                    .isInstanceOfSatisfying(DomainException.class,
                            error -> assertThat(error.code()).isEqualTo(ErrorCode.VALIDATION));
        }
        assertThat(service.get(order.getId()).units()).isEqualTo(10);
    }

    @Test
    void shouldRefuseToResizeAnOrderThatIsNotMoved() {
        Order order = OrderFixtures.save(orders, OUTLET, OrderStatus.CONFIRMED);

        assertThatThrownBy(() -> service.resizeMoved(order.getId(), 4))
                .isInstanceOfSatisfying(DomainException.class,
                        error -> assertThat(error.code()).isEqualTo(ErrorCode.INVALID_STATUS));
    }

    @Test
    void shouldCancelAMovedOrderOfAClosedRunAndWriteHistory() {
        Order order = OrderFixtures.save(orders, OUTLET, OrderStatus.MOVED);
        closeRunOf(order);

        OrderDto cancelled = service.cancelMoved(order.getId(), "Store cancelled after deferral");

        assertThat(cancelled.status()).isEqualTo(OrderStatus.CANCELLED);
        OrderEventDto event = lastEvent(order.getId());
        assertThat(event.type()).isEqualTo("CANCELLED");
        assertThat(event.fromStatus()).isEqualTo(OrderStatus.MOVED);
        assertThat(event.details()).containsEntry("reason", "Store cancelled after deferral");
    }

    @Test
    void shouldRefuseToCancelAsMovedWhenTheOrderIsNotMoved() {
        Order order = OrderFixtures.save(orders, OUTLET, OrderStatus.CONFIRMED);

        assertThatThrownBy(() -> service.cancelMoved(order.getId(), "x"))
                .isInstanceOfSatisfying(DomainException.class,
                        error -> assertThat(error.code()).isEqualTo(ErrorCode.INVALID_STATUS));
        assertThat(service.get(order.getId()).status()).isEqualTo(OrderStatus.CONFIRMED);
    }

    private void closeRunOf(Order order) {
        SignedInUser.signOut();
        OrderFixtures.insertUser(jdbc, "usr-t-dispatch", "DISPATCHER", null);
        SignedInUser.asDispatcher("usr-t-dispatch");
        LocalDate runDate = order.getRunDate();
        closeOrders.close(runDate, CLOSED_DEPOT);
        SignedInUser.signOut();
        SignedInUser.asStoreManager(STORE_USER, OUTLET);
    }

    private OrderEventDto lastEvent(String orderId) {
        var history = service.history(orderId);
        return history.get(history.size() - 1);
    }
}
