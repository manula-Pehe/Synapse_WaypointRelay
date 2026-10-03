package com.synapse.waypoint.core.reference.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** The D2 fleet view: vehicles with availability, summary counts and who confirmed the fleet and when. */
public record FleetDto(List<VehicleDto> items, OffsetDateTime confirmedAt, String confirmedBy,
        FleetCountsDto counts) {

    public FleetDto {
        items = List.copyOf(items);
    }
}
