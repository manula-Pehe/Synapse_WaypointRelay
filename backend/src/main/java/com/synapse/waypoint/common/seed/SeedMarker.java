package com.synapse.waypoint.common.seed;

/** Remembers that the seed has completed, so it never runs twice. */
public interface SeedMarker {

    boolean isSeeded();

    void markSeeded();
}
