package com.synapse.waypoint.driver.entity;

/**
 * Why a delivery was short or did not happen. The driver picks one instead of typing, so the record
 * is the proof */
public enum DeliveryReason {
    STORE_CLOSED,
    NO_ACCESS,
    REFUSED,
    DAMAGED
}