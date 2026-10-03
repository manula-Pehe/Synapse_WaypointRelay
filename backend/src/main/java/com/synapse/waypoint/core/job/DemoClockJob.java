package com.synapse.waypoint.core.job;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.synapse.waypoint.common.time.DemoClock;

/**
 * Work that is due at a fixed demo-clock time of a run. {@link JobRunner} asks {@link #shouldRun}
 * on every tick and calls {@link #run} once per run and depot; subclasses only say when the job is
 * due and what it does.
 */
public abstract class DemoClockJob {

    /** Stable name, used in logs and in the completion record. */
    public abstract String name();

    /** The demo-clock instant from which the job is due for {@code runDate}. */
    public abstract Instant triggerAt(LocalDate runDate);

    /** Does the work; runs inside the runner's transaction, which also records completion. */
    public abstract JobOutcome run(JobRun run);

    public final boolean shouldRun(JobRun run, Instant now) {
        return !now.isBefore(triggerAt(run.runDate()));
    }

    protected static Instant at(LocalDate date, LocalTime time) {
        return date.atTime(time).atZone(DemoClock.ZONE).toInstant();
    }
}
