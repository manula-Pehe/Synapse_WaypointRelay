package com.synapse.waypoint.core.order.repository;

import java.math.BigDecimal;

/** Summed weight, volume and units over a set of orders; every value is null when the set is empty. */
public record OrderUnitTotals(BigDecimal weightKg, BigDecimal volumeM3, Long units) {

    public boolean hasUnits() {
        return units != null && units > 0 && weightKg != null && volumeM3 != null;
    }
}
