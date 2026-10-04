package com.synapse.waypoint.planning.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** {@code depot} may be left out by a dispatcher who works at one depot. */
public record CreatePlanRequest(@NotNull LocalDate runDate, @Size(max = 20) String depot) {
}
