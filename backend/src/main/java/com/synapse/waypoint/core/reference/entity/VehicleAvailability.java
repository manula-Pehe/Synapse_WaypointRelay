package com.synapse.waypoint.core.reference.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

/** A vehicle's availability on one run date (table {@code vehicle_availability}, V1). */
@Entity
@Table(name = "vehicle_availability")
public class VehicleAvailability {

    @EmbeddedId
    private VehicleAvailabilityId id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AvailabilityStatus status;

    @Column(length = 200)
    private String reason;

    @Column(name = "updated_by", length = 40)
    private String updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VehicleAvailability() {
        // for JPA
    }

    public VehicleAvailability(VehicleAvailabilityId id, AvailabilityStatus status, String reason, String updatedBy,
            Instant updatedAt) {
        this.id = id;
        this.status = status;
        this.reason = reason;
        this.updatedBy = updatedBy;
        this.updatedAt = updatedAt;
    }

    public VehicleAvailabilityId getId() {
        return id;
    }

    public AvailabilityStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
