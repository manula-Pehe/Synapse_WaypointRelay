package com.synapse.waypoint.planning.dto;

import java.time.LocalDate;
import java.util.List;

import com.synapse.waypoint.planning.domain.PlanStatus;

/** The plan object of docs/api.md §6. */
public record PlanDto(String id, LocalDate runDate, String depot, int version, PlanStatus status,
        PlanSummaryDto summary, List<PlanVehicleDto> vehicles) {

    public PlanDto {
        vehicles = List.copyOf(vehicles);
    }
}
