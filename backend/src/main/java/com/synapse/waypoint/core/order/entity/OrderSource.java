package com.synapse.waypoint.core.order.entity;

/** How an order entered the system. */
public enum OrderSource {
    SEED,
    HISTORY,
    STORE,
    PHONE_IN,
    REMAINDER
}
