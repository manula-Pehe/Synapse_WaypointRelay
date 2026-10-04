package com.synapse.waypoint.planning.domain;

/** UNAVOIDABLE = no vehicle can carry the order; CHOSEN = it lost on priority. */
public enum DeferralKind {
    UNAVOIDABLE,
    CHOSEN
}
