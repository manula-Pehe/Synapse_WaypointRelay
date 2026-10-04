package com.synapse.waypoint.driver.entity;

/**
 * How a dispatcher settled a conflict (docs/api.md §9, D8m).
 *
 * <p>{@code KEEP_FIELD} means the driver's record stands - which is the default outcome, because a
 * delivery with proof is a physical fact and the board is not. {@code OVERRIDE} is the rare case
 * where the driver was wrong.
 */
public enum ConflictResolution {
    KEEP_FIELD,
    OVERRIDE
}