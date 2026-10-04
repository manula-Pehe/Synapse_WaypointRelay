package com.synapse.waypoint.core.job;

import java.time.LocalDate;

/** The unit a job runs for at most once: one delivery run at one depot. */
public record JobRun(LocalDate runDate, String depot) {
}
