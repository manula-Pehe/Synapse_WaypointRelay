package com.synapse.waypoint.core.reference.entity;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Litres a vehicle has used in one ISO week (table {@code fuel_usage}, V1). Read-only here. */
@Entity
@Table(name = "fuel_usage")
public class FuelUsage {

    @EmbeddedId
    private FuelUsageId id;

    @Column(name = "litres_used", nullable = false)
    private BigDecimal litresUsed;

    protected FuelUsage() {
        // for JPA
    }

    public FuelUsage(FuelUsageId id, BigDecimal litresUsed) {
        this.id = id;
        this.litresUsed = litresUsed;
    }

    public FuelUsageId getId() {
        return id;
    }

    public BigDecimal getLitresUsed() {
        return litresUsed;
    }
}
