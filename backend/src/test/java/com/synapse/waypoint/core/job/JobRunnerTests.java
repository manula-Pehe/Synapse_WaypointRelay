package com.synapse.waypoint.core.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionOperations;

import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.reference.repository.OutletRepository;

class JobRunnerTests {

    private static final LocalDate RUN_DATE = LocalDate.parse("2026-10-01");
    private static final Instant THREE_PM = demo("2026-09-30T15:00:00+05:30");
    private static final Instant FOUR_PM = demo("2026-09-30T16:00:00+05:30");

    private final MutableDemoClock clock = new MutableDemoClock(demo("2026-09-30T14:00:00+05:30"));
    private final InMemoryCompletionLog completionLog = new InMemoryCompletionLog();
    private final OutletRepository outlets = mock(OutletRepository.class);
    private final List<String> events = new ArrayList<>();

    @BeforeEach
    void knowTwoDepots() {
        when(outlets.findDepotNames()).thenReturn(List.of("Kandy", "Peliyagoda"));
    }

    @Test
    void shouldRunNothingBeforeTheTriggerTime() {
        JobRunner runner = runnerOf(new RecordingJob("reminder", THREE_PM));

        runner.tick();

        assertThat(events).isEmpty();
    }

    @Test
    void shouldRunADueJobOncePerDepotAndNeverAgain() {
        JobRunner runner = runnerOf(new RecordingJob("reminder", THREE_PM));
        clock.set(THREE_PM);

        runner.tick();
        runner.tick();
        clock.set(FOUR_PM);
        runner.tick();

        assertThat(events).containsExactly("reminder@Kandy", "reminder@Peliyagoda");
    }

    @Test
    void shouldCatchUpInTriggerTimeOrderWhenTheClockJumpsPastSeveralJobs() {
        JobRunner runner = runnerOf(new RecordingJob("close", FOUR_PM), new RecordingJob("reminder", THREE_PM));
        clock.set(demo("2026-09-30T16:30:00+05:30"));

        runner.tick();

        assertThat(events).containsExactly("reminder@Kandy", "close@Kandy", "reminder@Peliyagoda",
                "close@Peliyagoda");
    }

    @Test
    void shouldRecordASkippedJobAsDoneSoItDoesNotRunAgain() {
        RecordingJob job = new RecordingJob("alert", THREE_PM).skipping("nothing to report");
        JobRunner runner = runnerOf(job);
        clock.set(THREE_PM);

        runner.tick();
        runner.tick();

        assertThat(events).hasSize(2);
        assertThat(completionLog.isDone("alert", new JobRun(RUN_DATE, "Kandy"))).isTrue();
    }

    @Test
    void shouldKeepRunningOtherJobsWhenOneFailsAndRetryTheFailedOneOnTheNextTick() {
        RecordingJob failing = new RecordingJob("flaky", THREE_PM).failingTimes(1);
        RecordingJob steady = new RecordingJob("steady", FOUR_PM);
        JobRunner runner = runnerOf(failing, steady);
        clock.set(FOUR_PM);

        runner.tick();
        assertThat(events).contains("steady@Kandy", "steady@Peliyagoda").doesNotContain("flaky@Kandy");

        runner.tick();
        assertThat(events).contains("flaky@Kandy", "flaky@Peliyagoda");
        assertThat(events).filteredOn(e -> e.startsWith("steady")).hasSize(2);
    }

    @Test
    void shouldNotRecordCompletionWhenAJobFails() {
        JobRunner runner = runnerOf(new RecordingJob("flaky", THREE_PM).failingTimes(1));
        clock.set(THREE_PM);

        runner.tick();

        assertThat(completionLog.isDone("flaky", new JobRun(RUN_DATE, "Kandy"))).isFalse();
    }

    @Test
    void shouldNotRerunJobsWhenTheClockMovesBackwards() {
        JobRunner runner = runnerOf(new RecordingJob("reminder", THREE_PM));
        clock.set(FOUR_PM);
        runner.tick();

        clock.set(demo("2026-09-30T09:00:00+05:30"));
        runner.tick();
        clock.set(FOUR_PM);
        runner.tick();

        assertThat(events).hasSize(2);
    }

    private JobRunner runnerOf(DemoClockJob... jobs) {
        return new JobRunner(List.of(jobs), completionLog, clock, outlets, TransactionOperations.withoutTransaction());
    }

    private static Instant demo(String isoWithOffset) {
        return Instant.parse(java.time.OffsetDateTime.parse(isoWithOffset).toInstant().toString());
    }

    private final class RecordingJob extends DemoClockJob {

        private final String name;
        private final Instant trigger;
        private String skipReason;
        private int remainingFailures;

        RecordingJob(String name, Instant trigger) {
            this.name = name;
            this.trigger = trigger;
        }

        RecordingJob skipping(String reason) {
            this.skipReason = reason;
            return this;
        }

        RecordingJob failingTimes(int times) {
            this.remainingFailures = times * 2;
            return this;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Instant triggerAt(LocalDate runDate) {
            return trigger;
        }

        @Override
        public JobOutcome run(JobRun run) {
            if (remainingFailures > 0) {
                remainingFailures--;
                throw new IllegalStateException("boom");
            }
            events.add(name + "@" + run.depot());
            return skipReason == null ? JobOutcome.completed("done") : JobOutcome.skipped(skipReason);
        }
    }

    private static final class MutableDemoClock implements DemoClock {

        private Instant now;

        MutableDemoClock(Instant now) {
            this.now = now;
        }

        void set(Instant at) {
            this.now = at;
        }

        @Override
        public Instant now() {
            return now;
        }

        @Override
        public LocalDate runDate() {
            return RUN_DATE;
        }
    }

    private static final class InMemoryCompletionLog implements JobCompletionLog {

        private final Set<String> done = new HashSet<>();

        @Override
        public boolean isDone(String jobName, JobRun run) {
            return done.contains(key(jobName, run));
        }

        @Override
        public void markDone(String jobName, JobRun run, Instant at) {
            done.add(key(jobName, run));
        }

        private static String key(String jobName, JobRun run) {
            return jobName + "/" + run.runDate() + "/" + run.depot();
        }
    }
}
