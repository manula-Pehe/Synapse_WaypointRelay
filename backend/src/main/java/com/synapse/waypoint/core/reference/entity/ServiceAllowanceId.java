package com.synapse.waypoint.core.reference.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record ServiceAllowanceId(
        @Column(name = "brand", length = 10) String brand,
        @Column(name = "dock_type", length = 20) String dockType) implements Serializable {
}
