package com.synapse.waypoint.core.reference.entity;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record VehicleAvailabilityId(
        @Column(name = "vehicle_id", length = 10) String vehicleId,
        @Column(name = "run_date") LocalDate runDate) implements Serializable {
}
