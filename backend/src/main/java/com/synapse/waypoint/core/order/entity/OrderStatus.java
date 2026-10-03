package com.synapse.waypoint.core.order.entity;

/** Order lifecycle (docs/api.md, Enumerations). */
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
    CANCELLED
}
