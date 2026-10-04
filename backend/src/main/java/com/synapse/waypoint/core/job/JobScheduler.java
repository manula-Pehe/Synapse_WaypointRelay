package com.synapse.waypoint.core.job;

import org.springframework.scheduling.annotation.Scheduled;

/** Ticks the runner in real time; the runner decides from the demo clock what is due. */
class JobScheduler {

    private final JobRunner runner;

    JobScheduler(JobRunner runner) {
        this.runner = runner;
    }

    @Scheduled(initialDelayString = "${app.jobs.tick-interval:30s}",
            fixedDelayString = "${app.jobs.tick-interval:30s}")
    void tick() {
        runner.tick();
    }
}
