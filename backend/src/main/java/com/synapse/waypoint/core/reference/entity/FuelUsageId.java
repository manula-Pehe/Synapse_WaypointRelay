package com.synapse.waypoint.core.reference.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record FuelUsageId(
        @Column(name = "vehicle_id", length = 10) String vehicleId,
        @Column(name = "iso_year") int isoYear,
        @Column(name = "iso_week") int isoWeek) implements Serializable {
}
