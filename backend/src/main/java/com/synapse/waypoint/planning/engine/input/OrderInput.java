package com.synapse.waypoint.planning.engine.input;

import java.math.BigDecimal;

import com.synapse.waypoint.planning.domain.Brand;
import com.synapse.waypoint.planning.domain.Temperature;

/** A confirmed order the plan must place or defer. */
public record OrderInput(String id, String ref, String outletId, Brand brand, Temperature temperature,
        BigDecimal weightKg, BigDecimal volumeM3, int daysSinceLastServed, boolean deferredYesterday) {

    public boolean isChilled() {
        return temperature == Temperature.CHILLED;
    }
}
