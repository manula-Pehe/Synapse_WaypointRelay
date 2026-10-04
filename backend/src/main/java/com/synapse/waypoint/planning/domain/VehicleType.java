package com.synapse.waypoint.planning.domain;

import java.util.Locale;

/** Body type; vans reach outlets that trucks cannot. */
public enum VehicleType {
    TRUCK,
    VAN;

    public static VehicleType fromValue(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
