package com.synapse.waypoint.core.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class JobCompletionLogTests {

    private static final JobRun RUN = new JobRun(LocalDate.parse("2026-10-01"), "Testdepot");

    @Autowired JobCompletionLog completionLog;

    @Test
    void shouldRememberACompletedJobPerRunAndDepot() {
        completionLog.markDone("close-orders", RUN, Instant.parse("2026-09-30T10:30:00Z"));

        assertThat(completionLog.isDone("close-orders", RUN)).isTrue();
        assertThat(completionLog.isDone("close-orders", new JobRun(RUN.runDate(), "Otherdepot"))).isFalse();
        assertThat(completionLog.isDone("close-orders", new JobRun(RUN.runDate().plusDays(1), "Testdepot")))
                .isFalse();
        assertThat(completionLog.isDone("store-reminder", RUN)).isFalse();
    }

    @Test
    void shouldRejectAKeyThatDoesNotFitTheSettingsColumn() {
        JobRun longDepot = new JobRun(RUN.runDate(), "A depot with a very long name that cannot fit");

        assertThatThrownBy(() -> completionLog.markDone("close-orders", longDepot, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
