package com.synapse.waypoint.core.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.order.support.OrderFixtures;

/**
 * Two people changing the same order: the second save must fail instead of overwriting the first.
 * Runs without a wrapping transaction (the conflict needs two real ones) and cleans up after itself.
 */
@SpringBootTest
class OrderConcurrentChangeTests {

    private static final String OUTLET = "OUT993";

    @Autowired OrderService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired OrderRepository orders;
    @MockitoSpyBean OrderChangeGuard changeGuard;

    @BeforeEach
    void createOutlet() {
        OrderFixtures.insertOutlet(jdbc, OUTLET, "Fresh", "Testdistrict");
    }

    @AfterEach
    void removeTestData() {
        jdbc.update("DELETE FROM order_events WHERE order_id IN (SELECT id FROM orders WHERE outlet_id = ?)", OUTLET);
        jdbc.update("DELETE FROM orders WHERE outlet_id = ?", OUTLET);
        jdbc.update("DELETE FROM outlets WHERE id = ?", OUTLET);
    }

    @Test
    void shouldRejectTheLaterChangeWhenSomeoneElseChangedTheOrderFirst() {
        Order order = OrderFixtures.save(orders, OUTLET, OrderStatus.PREPARED);
        // The guard runs after the order is loaded and before it is saved: change it behind the service's back.
        doAnswer(invocation -> {
            jdbc.update("UPDATE orders SET version = version + 1 WHERE id = ?", order.getId());
            return null;
        }).when(changeGuard).requireOpenForChanges(any(Order.class));

        assertThatThrownBy(() -> service.confirm(order.getId()))
                .isInstanceOf(OptimisticLockingFailureException.class);

        Integer events = jdbc.queryForObject("SELECT count(*) FROM order_events WHERE order_id = ?", Integer.class,
                order.getId());
        assertThat(events).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE id = ?", String.class, order.getId()))
                .isEqualTo("PREPARED");
    }
}
