package com.synapse.waypoint.planning.domain;

/** What a store decides for a deferred order. */
public enum StoreChoice {
    KEEP,
    REDUCE,
    CANCEL,
    SPLIT
}
