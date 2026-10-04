package com.synapse.waypoint.driver.dto;

import java.time.OffsetDateTime;
import java.util.List;

/** One trip of a driver's run, with its stops already in delivery order (docs/api.md §7, R1). */
public record DriverTripDto(
        String id,
        int tripNo,
        String brand,
        String district,
        OffsetDateTime departAt,
        List<DriverStopDto> stops) {

    public DriverTripDto {
        stops = List.copyOf(stops);
    }
}