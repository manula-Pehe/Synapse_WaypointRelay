package com.synapse.waypoint.planning.dto;

import java.util.List;

/** What the dispatcher still has to do before a plan can be created (Dp0). */
public record PlanReadinessDto(boolean ordersClosed, boolean fleetConfirmed, int confirmedOrders,
        int availableVehicles, int reeferAvailable, List<String> warnings) {

    public PlanReadinessDto {
        warnings = List.copyOf(warnings);
    }
}
