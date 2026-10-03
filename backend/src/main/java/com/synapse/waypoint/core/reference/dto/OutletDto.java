package com.synapse.waypoint.core.reference.dto;

import java.time.LocalTime;

/** An outlet as shown on screens (D13) and read by the planner; {@code name} is "OUT001 · District". */
public record OutletDto(String id, String name, String brand, String district, String depot, String dockType,
        String parkingConstraint, LocalTime windowOpen, LocalTime windowClose, LocalTime mallWindowOpen,
        LocalTime mallWindowClose) {
}
