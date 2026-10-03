package com.synapse.waypoint.core.order.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * One delivery run of one depot (table {@code order_runs}, V1). Only the order cut-off is mapped
 * here; the fleet confirmation columns belong to the fleet feature.
 */
@Entity
@Table(name = "order_runs")
public class OrderRun {

    @EmbeddedId
    private OrderRunId id;

    @Column(name = "orders_closed_at")
    private Instant ordersClosedAt;

    @Column(name = "orders_closed_by", length = 40)
    private String ordersClosedBy;

    protected OrderRun() {
        // for JPA
    }

    public boolean isClosed() {
        return ordersClosedAt != null;
    }

    /** Closes the orders; {@code userId} is null when a timed job closes them. */
    public void closeOrders(String userId, Instant now) {
        ordersClosedAt = now;
        ordersClosedBy = userId;
    }

    public OrderRunId getId() {
        return id;
    }

    public Instant getOrdersClosedAt() {
        return ordersClosedAt;
    }

    public String getOrdersClosedBy() {
        return ordersClosedBy;
    }
}
