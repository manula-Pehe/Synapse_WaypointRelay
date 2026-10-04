package com.synapse.waypoint.driver.entity;

/** What the server did with one queued action */
public enum SyncResult {
    /** The action was carried out. */
    APPLIED,
    /** We had already carried this clientId out - a retry, applied once. */
    DUPLICATE,
    /** Applied, but it clashed with a dispatcher's change; they get a decision card. */
    CONFLICT
}