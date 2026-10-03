package com.synapse.waypoint.core.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.order.entity.NewOrder;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderEvent;
import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.repository.OrderEventRepository;
import com.synapse.waypoint.core.order.repository.OrderRepository;
import com.synapse.waypoint.core.reference.entity.Outlet;
import com.synapse.waypoint.core.reference.repository.OutletRepository;

/** Entities must match V1 exactly (ddl-auto=validate); values are invented. */
@SpringBootTest
@Transactional
class OrderEntitiesPersistenceTests {

    private static final Instant NOW = Instant.parse("2026-09-30T08:30:00Z");

    @Autowired OrderRepository orders;
    @Autowired OrderEventRepository events;
    @Autowired OutletRepository outlets;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void createOutlet() {
        outlets.saveAndFlush(new Outlet("OUT991", "Fresh", "Testdistrict", "Peliyagoda", "rear_dock", "normal", null,
                null, LocalTime.of(5, 0), LocalTime.of(7, 0)));
    }

    @Test
    void shouldWriteVersionZeroOnFirstInsert() {
        orders.saveAndFlush(Order.create(newOrder("order-1"), NOW));

        Integer version = jdbc.queryForObject("SELECT version FROM orders WHERE id = 'order-1'", Integer.class);

        assertThat(version).isZero();
    }

    @Test
    void shouldReloadOrderWithAllFields() {
        orders.saveAndFlush(Order.create(newOrder("order-2"), NOW));

        Order loaded = orders.findById("order-2").orElseThrow();

        assertThat(loaded.getStatus()).isEqualTo(OrderStatus.PREPARED);
        assertThat(loaded.getTemperatureRequirement()).isEqualTo(TemperatureRequirement.CHILLED);
        assertThat(loaded.getWeightKg()).isEqualByComparingTo("12.5");
        assertThat(loaded.getCreatedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldSaveEventsInOrderWithJsonDetails() {
        orders.saveAndFlush(Order.create(newOrder("order-3"), NOW));
        events.saveAndFlush(new OrderEvent("order-3", NOW, null, "PREPARED", null, OrderStatus.PREPARED, Map.of()));
        events.saveAndFlush(new OrderEvent("order-3", NOW.plusSeconds(60), null, "EDITED", OrderStatus.PREPARED,
                OrderStatus.PREPARED, Map.of("units", 5)));

        var history = events.findByOrderIdOrderByAtAscIdAsc("order-3");

        assertThat(history).extracting(OrderEvent::getType).containsExactly("PREPARED", "EDITED");
        assertThat(history.get(1).getDetails()).containsEntry("units", 5);
        assertThat(history.get(0).getActorUserId()).isNull();
    }

    private NewOrder newOrder(String id) {
        return new NewOrder(id, "T-" + id, "OUT991", "Fresh", TemperatureRequirement.CHILLED, 10,
                new BigDecimal("12.5"), new BigDecimal("0.250"), LocalDate.parse("2026-10-01"), OrderStatus.PREPARED,
                OrderSource.SEED, false, 1, false, null, true);
    }
}
