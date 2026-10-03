package com.synapse.waypoint.common.seed;

/**
 * One part of the first-start data load. Each module contributes its own steps; the
 * {@link SeedRunner} runs them in ascending {@link #order()} inside a single transaction.
 */
public interface SeedStep {

    /** Position in the run; a step may depend on rows written by steps with a lower number. */
    int order();

    /** Short name used in the start-up log. */
    String name();

    void run();
}
