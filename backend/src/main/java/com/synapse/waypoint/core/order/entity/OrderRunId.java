package com.synapse.waypoint.core.order.entity;

import java.io.Serializable;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public record OrderRunId(
        @Column(name = "run_date") LocalDate runDate,
        @Column(name = "depot", length = 20) String depot) implements Serializable {
}
