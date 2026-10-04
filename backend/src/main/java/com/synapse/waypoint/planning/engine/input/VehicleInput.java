package com.synapse.waypoint.planning.engine.input;

import java.math.BigDecimal;

import com.synapse.waypoint.planning.domain.VehicleTemperature;
import com.synapse.waypoint.planning.domain.VehicleType;

/** An available vehicle with its capacity and the fuel it has already used this week. */
public record VehicleInput(String id, String depot, VehicleType type, VehicleTemperature temperature,
        BigDecimal weightCapKg, BigDecimal volumeCapM3, BigDecimal kmPerLitre, BigDecimal weeklyQuotaLitres,
        BigDecimal fuelUsedThisWeekLitres) {

    public boolean isReefer() {
        return temperature == VehicleTemperature.REEFER;
    }
}
