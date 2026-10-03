package com.synapse.waypoint.core.reference.dto;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * An outlet as shown on screens (D13) and read by the planner; {@code name} is "OUT001 · District".
 * Window times are wall-clock {@code HH:mm} (docs/api.md, Conventions).
 */
public record OutletDto(String id, String name, String brand, String district, String depot, String dockType,
        String parkingConstraint,
        @JsonFormat(pattern = OutletDto.WALL_CLOCK) LocalTime windowOpen,
        @JsonFormat(pattern = OutletDto.WALL_CLOCK) LocalTime windowClose,
        @JsonFormat(pattern = OutletDto.WALL_CLOCK) LocalTime mallWindowOpen,
        @JsonFormat(pattern = OutletDto.WALL_CLOCK) LocalTime mallWindowClose) {

    static final String WALL_CLOCK = "HH:mm";
}
