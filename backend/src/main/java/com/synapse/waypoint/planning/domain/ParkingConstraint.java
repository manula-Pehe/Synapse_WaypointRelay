package com.synapse.waypoint.planning.domain;

import java.util.Arrays;

/** Access limits at an outlet. */
public enum ParkingConstraint {
    NORMAL("normal"),
    VAN_ONLY("van_only"),
    MALL_DOCK("mall_dock");

    private final String value;

    ParkingConstraint(String value) {
        this.value = value;
    }

    public static ParkingConstraint fromValue(String value) {
        return Arrays.stream(values())
                .filter(constraint -> constraint.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown parking constraint: " + value));
    }
}
