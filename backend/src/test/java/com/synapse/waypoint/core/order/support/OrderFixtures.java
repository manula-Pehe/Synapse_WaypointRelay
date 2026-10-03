package com.synapse.waypoint.core.order.support;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;

import com.synapse.waypoint.core.order.entity.NewOrder;
import com.synapse.waypoint.core.order.entity.Order;
import com.synapse.waypoint.core.order.entity.OrderSource;
import com.synapse.waypoint.core.order.entity.OrderStatus;
import com.synapse.waypoint.core.order.entity.TemperatureRequirement;
import com.synapse.waypoint.core.order.repository.OrderRepository;

/** Invented test data for order tests. */
public final class OrderFixtures {

    public static final String DEPOT = "Testdepot";
    public static final LocalDate RUN_DATE = LocalDate.parse("2026-10-01");
    public static final Instant CREATED_AT = Instant.parse("2026-09-30T08:30:00Z");

    private OrderFixtures() {
    }

    public static void insertOutlet(JdbcTemplate jdbc, String id, String brand, String district) {
        insertOutlet(jdbc, id, brand, district, DEPOT);
    }

    public static void insertOutlet(JdbcTemplate jdbc, String id, String brand, String district, String depot) {
        jdbc.update("""
                INSERT INTO outlets (id, brand, district, depot, dock_type, parking_constraint, window_open, window_close)
                VALUES (?, ?, ?, ?, 'rear_dock', 'normal', '05:00', '07:00')""", id, brand, district, depot);
    }

    public static void insertUser(JdbcTemplate jdbc, String id, String role, String outletId) {
        jdbc.update("""
                INSERT INTO users (id, name, role, email, secret_hash, outlet_id, active)
                VALUES (?, ?, ?, ?, 'x', ?, true)""", id, "Test " + role, role, id + "@example.lk", outletId);
    }

    public static Order save(OrderRepository orders, String outletId, OrderStatus status) {
        return orders.saveAndFlush(Order.create(newOrder(outletId, status, TemperatureRequirement.AMBIENT, 10,
                OrderSource.STORE, RUN_DATE), CREATED_AT));
    }

    public static Order save(OrderRepository orders, String outletId, String brand, TemperatureRequirement temp,
            OrderStatus status) {
        return orders.saveAndFlush(Order.create(newOrder(outletId, brand, status, temp, 10, OrderSource.SEED,
                RUN_DATE), CREATED_AT));
    }

    public static NewOrder newOrder(String outletId, OrderStatus status, TemperatureRequirement temp, int units,
            OrderSource source, LocalDate runDate) {
        return newOrder(outletId, "Fresh", status, temp, units, source, runDate);
    }

    public static NewOrder newOrder(String outletId, String brand, OrderStatus status, TemperatureRequirement temp,
            int units, OrderSource source, LocalDate runDate) {
        String id = UUID.randomUUID().toString();
        return new NewOrder(id, "T-" + id.substring(0, 8), outletId, brand, temp, units,
                new BigDecimal("100.00"), new BigDecimal("1.000"), runDate, status, source, false, 1, false, null,
                true);
    }
}
