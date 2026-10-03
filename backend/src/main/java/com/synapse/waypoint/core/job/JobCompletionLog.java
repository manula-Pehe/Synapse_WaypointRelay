package com.synapse.waypoint.core.job;

import java.time.Instant;

/** Durable record of which job already ran for which run, so a restart never repeats it. */
public interface JobCompletionLog {

    boolean isDone(String jobName, JobRun run);

    void markDone(String jobName, JobRun run, Instant at);
}
