package com.synapse.waypoint.core.reference.dto;

import java.util.List;

import com.synapse.waypoint.core.reference.entity.AvailabilityStatus;

/** The D2 summary: vehicles per availability status, and how many available vehicles are reefers. */
public record FleetCountsDto(int available, int inWorkshop, int offRoad, int reeferAvailable) {

    private static final String REEFER = "reefer";

    public static FleetCountsDto of(List<VehicleDto> vehicles) {
        return new FleetCountsDto(
                count(vehicles, AvailabilityStatus.AVAILABLE),
                count(vehicles, AvailabilityStatus.IN_WORKSHOP),
                count(vehicles, AvailabilityStatus.OFF_ROAD),
                (int) vehicles.stream()
                        .filter(v -> v.availability() == AvailabilityStatus.AVAILABLE && REEFER.equals(v.temp()))
                        .count());
    }

    private static int count(List<VehicleDto> vehicles, AvailabilityStatus status) {
        return (int) vehicles.stream().filter(v -> v.availability() == status).count();
    }
}
