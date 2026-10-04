package com.synapse.waypoint.planning.dto;

import java.math.BigDecimal;
import java.util.List;

/** A vehicle of the plan with its trips and the minutes it uses against each time budget. */
public record PlanVehicleDto(String vehicleId, String type, String temp, BigDecimal weightCapKg,
        BigDecimal volumeCapM3, int freshMinutesUsed, int freshBudget, int daytimeMinutesUsed, int daytimeBudget,
        List<PlanTripDto> trips) {

    public PlanVehicleDto {
        trips = List.copyOf(trips);
    }
}
