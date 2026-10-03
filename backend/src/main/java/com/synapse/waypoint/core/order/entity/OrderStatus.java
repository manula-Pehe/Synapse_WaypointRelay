package com.synapse.waypoint.core.order.entity;

import java.util.Set;

/**
 * Order lifecycle (docs/api.md, "Order lifecycle"). The allowed transitions are defined here and
 * nowhere else.
 */
public enum OrderStatus {
    PREPARED,
    CONFIRMED,
    PLANNED,
    LOADED,
    ON_THE_WAY,
    DELIVERED,
    PARTIAL,
    FAILED,
    MOVED,
    CANCELLED;

    /** The statuses an order in this status may move to; empty for end states. */
    public Set<OrderStatus> allowedNext() {
        return switch (this) {
            case PREPARED -> Set.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> Set.of(PLANNED, MOVED, CANCELLED);
            case PLANNED -> Set.of(LOADED, MOVED);
            case MOVED -> Set.of(CONFIRMED, CANCELLED);
            case LOADED -> Set.of(ON_THE_WAY);
            case ON_THE_WAY -> Set.of(DELIVERED, PARTIAL, FAILED);
            case FAILED -> Set.of(MOVED, CANCELLED);
            case DELIVERED, PARTIAL, CANCELLED -> Set.of();
        };
    }

    public boolean canMoveTo(OrderStatus target) {
        return allowedNext().contains(target);
    }
}
