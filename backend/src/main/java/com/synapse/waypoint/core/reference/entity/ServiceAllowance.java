package com.synapse.waypoint.core.reference.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Minutes spent at a stop for a brand and dock type (table {@code service_allowance}, V1). */
@Entity
@Table(name = "service_allowance")
public class ServiceAllowance {

    @EmbeddedId
    private ServiceAllowanceId id;

    @Column(name = "minutes", nullable = false)
    private int minutes;

    protected ServiceAllowance() {
        // for JPA
    }

    public ServiceAllowance(ServiceAllowanceId id, int minutes) {
        this.id = id;
        this.minutes = minutes;
    }

    public ServiceAllowanceId getId() {
        return id;
    }

    public int getMinutes() {
        return minutes;
    }
}
