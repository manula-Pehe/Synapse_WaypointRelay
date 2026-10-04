package com.synapse.waypoint.core.job;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionOperations;

import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.reference.repository.OutletRepository;

/**
 * Runs every due {@link DemoClockJob} once per run date and depot, in the order of their trigger
 * times. Each job runs in its own transaction together with its completion record; a failing job
 * is logged and retried on the next tick without stopping the others.
 */
public class JobRunner {

    private static final Logger log = LoggerFactory.getLogger(JobRunner.class);

    private final List<DemoClockJob> jobs;
    private final JobCompletionLog completionLog;
    private final DemoClock clock;
    private final OutletRepository outlets;
    private final TransactionOperations transactionPerJob;
    private final ReentrantLock tickLock = new ReentrantLock();

    public JobRunner(List<DemoClockJob> jobs, JobCompletionLog completionLog, DemoClock clock,
            OutletRepository outlets, TransactionOperations transactionPerJob) {
        this.jobs = List.copyOf(jobs);
        this.completionLog = completionLog;
        this.clock = clock;
        this.outlets = outlets;
        this.transactionPerJob = transactionPerJob;
    }

    /** Runs what is due now; concurrent ticks (scheduler, clock move) are serialised. */
    public void tick() {
        tickLock.lock();
        try {
            runDueJobs();
        } catch (RuntimeException e) {
            log.warn("Job tick failed before any job could run", e);
        } finally {
            tickLock.unlock();
        }
    }

    private void runDueJobs() {
        Instant now = clock.now();
        LocalDate runDate = clock.runDate();
        List<DemoClockJob> inTimeOrder = jobs.stream()
                .sorted(Comparator.comparing((DemoClockJob job) -> job.triggerAt(runDate)))
                .toList();
        for (String depot : outlets.findDepotNames()) {
            JobRun run = new JobRun(runDate, depot);
            for (DemoClockJob job : inTimeOrder) {
                if (job.shouldRun(run, now) && !completionLog.isDone(job.name(), run)) {
                    fire(job, run, now);
                }
            }
        }
    }

    private void fire(DemoClockJob job, JobRun run, Instant now) {
        try {
            JobOutcome outcome = transactionPerJob.execute(status -> {
                JobOutcome result = job.run(run);
                completionLog.markDone(job.name(), run, now);
                return result;
            });
            logOutcome(job, run, outcome);
        } catch (RuntimeException e) {
            log.warn("Job {} failed (runDate={}, depot={}); it will retry on the next tick", job.name(),
                    run.runDate(), run.depot(), e);
        }
    }

    private static void logOutcome(DemoClockJob job, JobRun run, JobOutcome outcome) {
        if (outcome.isSkipped()) {
            log.info("Job {} skipped – {} (runDate={}, depot={})", job.name(), outcome.detail(), run.runDate(),
                    run.depot());
            return;
        }
        log.info("Job {} fired – {} (runDate={}, depot={})", job.name(), outcome.detail(), run.runDate(),
                run.depot());
    }
}
