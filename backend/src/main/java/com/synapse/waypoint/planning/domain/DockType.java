package com.synapse.waypoint.planning.domain;

import java.util.Arrays;

/** Where a vehicle unloads at an outlet; service time depends on it. */
public enum DockType {
    REAR_DOCK("rear_dock"),
    STREET("street"),
    MALL_BAY("mall_bay");

    private final String value;

    DockType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static DockType fromValue(String value) {
        return Arrays.stream(values())
                .filter(type -> type.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown dock type: " + value));
    }
}
