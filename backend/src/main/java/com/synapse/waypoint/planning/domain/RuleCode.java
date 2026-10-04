package com.synapse.waypoint.planning.domain;

/** The planning rule that decided a violation or a deferral (docs/api.md §0). */
public enum RuleCode {
    BRAND_DISTRICT,
    FRIDGE_REQUIRED,
    VAN_ONLY,
    WRONG_DEPOT,
    OVER_WEIGHT,
    OVER_VOLUME,
    MAX_TRIPS,
    TIME_BUDGET,
    FUEL_QUOTA,
    WINDOW,
    /** Deferral only: no available vehicle could carry the order. */
    NO_VEHICLE_FITS,
    /** Deferral only: every fridge vehicle is already full. */
    FRIDGE_CAPACITY
}
