package com.synapse.waypoint.planning.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import com.synapse.waypoint.planning.domain.TripWindowType;

/** One vehicle leaving the depot for one brand and district, with its stops in delivery order. */
public record PlanTripDto(String id, int tripNo, String brand, String district, TripWindowType windowType,
        OffsetDateTime departAt, int minutes, BigDecimal weightKg, BigDecimal volumeM3, List<PlanStopDto> stops) {

    public PlanTripDto {
        stops = List.copyOf(stops);
    }
}
