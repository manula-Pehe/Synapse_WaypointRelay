package com.synapse.waypoint.core.job;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.beans.factory.ObjectProvider;

/**
 * A run-day job that hands its work to another module through a hook interface. When that module
 * has no implementation yet, the job skips itself and still counts as done.
 */
abstract class RunDayHookJob<T> extends DemoClockJob {

    private final ObjectProvider<T> hook;
    private final LocalTime triggerTime;

    protected RunDayHookJob(ObjectProvider<T> hook, LocalTime triggerTime) {
        this.hook = hook;
        this.triggerTime = triggerTime;
    }

    protected abstract void invoke(T implementation, JobRun run);

    @Override
    public final Instant triggerAt(LocalDate runDate) {
        return at(runDate, triggerTime);
    }

    @Override
    public final JobOutcome run(JobRun run) {
        T implementation = hook.getIfAvailable();
        if (implementation == null) {
            return JobOutcome.skipped("no implementation");
        }
        invoke(implementation, run);
        return JobOutcome.completed("handed over to " + implementation.getClass().getSimpleName());
    }
}
