package com.synapse.waypoint.core.reference.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record DistrictTravelId(
        @Column(name = "district", length = 40) String district,
        @Column(name = "depot", length = 20) String depot) implements Serializable {
}
