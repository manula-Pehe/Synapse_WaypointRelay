package com.synapse.waypoint.core.order.event;

import java.time.Instant;

import com.synapse.waypoint.core.order.entity.OrderStatus;

/**
 * Published inside the transaction of every order change (creation included), after the history row
 * is written. Listeners that must only react to committed changes use
 * {@code @TransactionalEventListener}. {@code from} is null for a new order; {@code actorUserId} is
 * null for system changes; {@code type} is the history row type (a status name, or {@code EDITED}).
 */
public record OrderStatusChanged(
        String orderId,
        String outletId,
        String type,
        OrderStatus from,
        OrderStatus to,
        String actorUserId,
        Instant at) {
}
