package com.synapse.waypoint.driver.entity;

/**
 * Why a delivery was short or did not happen. The driver picks one instead of typing, so the record
 * is the proof (docs/briefs driver F6, screens R4c and R4d).
 */
public enum DeliveryReason {
    STORE_CLOSED,
    NO_ACCESS,
    REFUSED,
    DAMAGED
}