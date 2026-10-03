package com.synapse.waypoint.core.reference.dto;

import java.math.BigDecimal;

import com.synapse.waypoint.core.reference.entity.AvailabilityStatus;

/** A vehicle with its availability on one run date. */
public record VehicleDto(String id, String type, String temp, BigDecimal weightCapKg, BigDecimal volumeCapM3,
        String fuelType, BigDecimal kmPerL, BigDecimal weeklyFuelQuotaL, String depot,
        AvailabilityStatus availability, String availabilityReason) {
}
