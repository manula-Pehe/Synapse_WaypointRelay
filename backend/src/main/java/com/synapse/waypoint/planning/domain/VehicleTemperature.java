package com.synapse.waypoint.planning.domain;

import java.util.Locale;

/** A reefer is refrigerated and may also carry ambient goods. */
public enum VehicleTemperature {
    REEFER,
    AMBIENT;

    public static VehicleTemperature fromValue(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
