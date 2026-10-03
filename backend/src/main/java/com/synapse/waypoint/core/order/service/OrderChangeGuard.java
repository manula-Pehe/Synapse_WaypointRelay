package com.synapse.waypoint.core.order.service;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.core.order.entity.Order;

/**
 * The single place that decides whether a store may still change an order. The cut-off rule
 * (ORDERS_CLOSED) is added here by F6; until then every change is allowed.
 */
@Component
class OrderChangeGuard {

    void requireOpenForChanges(Order order) {
        // F6 · close orders: refuse here once the run is closed.
    }
}
