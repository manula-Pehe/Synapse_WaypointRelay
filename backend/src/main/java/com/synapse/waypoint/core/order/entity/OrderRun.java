package com.synapse.waypoint.core.order.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** One delivery run of one depot (table {@code order_runs}, V1): the order cut-off and the fleet confirmation. */
@Entity
@Table(name = "order_runs")
public class OrderRun {

    @EmbeddedId
    private OrderRunId id;

    @Column(name = "orders_closed_at")
    private Instant ordersClosedAt;

    @Column(name = "orders_closed_by", length = 40)
    private String ordersClosedBy;

    @Column(name = "fleet_confirmed_at")
    private Instant fleetConfirmedAt;

    @Column(name = "fleet_confirmed_by", length = 40)
    private String fleetConfirmedBy;

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

    /** Confirms the fleet; confirming again just moves the time. */
    public void confirmFleet(String userId, Instant now) {
        fleetConfirmedAt = now;
        fleetConfirmedBy = userId;
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

    public Instant getFleetConfirmedAt() {
        return fleetConfirmedAt;
    }

    public String getFleetConfirmedBy() {
        return fleetConfirmedBy;
    }
}
