package com.synapse.waypoint.driver.entity;

/**
 * The actions a phone may queue for {@code POST /api/sync} (docs/api.md §9). The loader app adds
 * {@code LOAD_TICK}, {@code SHORTFALL} and {@code HANDOVER}.
 */
public enum SyncActionType {
    TRIP_ACCEPTED,
    ARRIVED,
    DELIVERY_RECORDED,
    DELIVERY_UNDONE,
    STORE_WAIT,
    VEHICLE_PROBLEM
}