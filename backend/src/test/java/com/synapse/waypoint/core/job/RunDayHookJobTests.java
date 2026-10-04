package com.synapse.waypoint.core.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import com.synapse.waypoint.core.job.hook.FailedDeliveryRetry;
import com.synapse.waypoint.core.job.hook.ReceiptAutoClose;

class RunDayHookJobTests {

    private static final JobRun RUN = new JobRun(LocalDate.parse("2026-10-01"), "Testdepot");

    @Test
    void shouldRetryFailedDeliveriesAt2PmOnTheRunDay() {
        FailedDeliveryRetryJob job = new FailedDeliveryRetryJob(providerOf(mock(FailedDeliveryRetry.class)));

        assertThat(job.triggerAt(RUN.runDate())).isEqualTo(Instant.parse("2026-10-01T08:30:00Z"));
    }

    @Test
    void shouldAutoCloseReceiptsAt1159PmOnTheRunDay() {
        ReceiptAutoCloseJob job = new ReceiptAutoCloseJob(providerOf(mock(ReceiptAutoClose.class)));

        assertThat(job.triggerAt(RUN.runDate())).isEqualTo(Instant.parse("2026-10-01T18:29:00Z"));
    }

    @Test
    void shouldCallTheFailedDeliveryRetryWhenAnImplementationExists() {
        FailedDeliveryRetry retry = mock(FailedDeliveryRetry.class);

        JobOutcome outcome = new FailedDeliveryRetryJob(providerOf(retry)).run(RUN);

        verify(retry).replanUnansweredFailures(RUN.runDate(), RUN.depot());
        assertThat(outcome.isSkipped()).isFalse();
    }

    @Test
    void shouldCallTheReceiptAutoCloseWhenAnImplementationExists() {
        ReceiptAutoClose autoClose = mock(ReceiptAutoClose.class);

        JobOutcome outcome = new ReceiptAutoCloseJob(providerOf(autoClose)).run(RUN);

        verify(autoClose).closeUnconfirmedReceipts(RUN.runDate(), RUN.depot());
        assertThat(outcome.isSkipped()).isFalse();
    }

    @Test
    void shouldSkipWithoutAnImplementation() {
        JobOutcome retry = new FailedDeliveryRetryJob(providerOf(null)).run(RUN);
        JobOutcome autoClose = new ReceiptAutoCloseJob(providerOf(null)).run(RUN);

        assertThat(retry.isSkipped()).isTrue();
        assertThat(retry.detail()).isEqualTo("no implementation");
        assertThat(autoClose.isSkipped()).isTrue();
        assertThat(autoClose.detail()).isEqualTo("no implementation");
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> providerOf(T implementation) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(implementation);
        return provider;
    }
}
