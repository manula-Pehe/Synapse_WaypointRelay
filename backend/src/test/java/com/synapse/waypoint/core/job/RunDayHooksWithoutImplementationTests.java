package com.synapse.waypoint.core.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** The application as it is today: no module implements the run-day hooks yet. */
@SpringBootTest
class RunDayHooksWithoutImplementationTests {

    private static final JobRun RUN = new JobRun(LocalDate.parse("2026-10-01"), "Testdepot");

    @Autowired FailedDeliveryRetryJob retryJob;
    @Autowired ReceiptAutoCloseJob autoCloseJob;

    @Test
    void shouldSkipBothJobsInTheRunningApplication() {
        assertThat(retryJob.run(RUN).isSkipped()).isTrue();
        assertThat(autoCloseJob.run(RUN).isSkipped()).isTrue();
    }
}
