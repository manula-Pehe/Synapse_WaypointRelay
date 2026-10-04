package com.synapse.waypoint.driver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.synapse.waypoint.core.order.entity.DeliveryOutcome;
import com.synapse.waypoint.driver.entity.Delivery;
import com.synapse.waypoint.driver.entity.DeliveryReason;
import com.synapse.waypoint.driver.entity.FailedDeliveryDecision;
import com.synapse.waypoint.driver.repository.DeliveryRepository;

/** The 2 PM fallback must only touch failures nobody answered. Data is invented. */
class DriverFailedDeliveryRetryTests {

    private static final LocalDate RUN_DATE = LocalDate.parse("2030-01-10");
    private static final String DEPOT = "Testdepot";
    private static final Instant AT = Instant.parse("2030-01-10T05:00:00Z");

    private final DeliveryRepository deliveries = mock(DeliveryRepository.class);
    private final DriverFailedDeliveryRetry retry = new DriverFailedDeliveryRetry(deliveries);

    @Test
    void shouldReplanAFailureNobodyAnswered() {
        Delivery unanswered = failed("d-1");
        whenFailuresAre(unanswered);

        retry.replanUnansweredFailures(RUN_DATE, DEPOT);

        assertThat(unanswered.getDecision()).isEqualTo(FailedDeliveryDecision.REPLAN_TOMORROW);
    }

    @Test
    void shouldLeaveAFailureTheStoreAlreadyAnswered() {
        Delivery answered = failed("d-2");
        answered.decide(FailedDeliveryDecision.TRY_LATER_TODAY, "usr-store", AT);
        whenFailuresAre(answered);

        retry.replanUnansweredFailures(RUN_DATE, DEPOT);

        assertThat(answered.getDecision()).isEqualTo(FailedDeliveryDecision.TRY_LATER_TODAY);
        verify(deliveries, never()).saveAll(anyIterable());
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldSaveOnlyTheUnansweredOnes() {
        Delivery answered = failed("d-3");
        answered.decide(FailedDeliveryDecision.CANCEL, "usr-store", AT);
        Delivery unanswered = failed("d-4");
        whenFailuresAre(answered, unanswered);

        retry.replanUnansweredFailures(RUN_DATE, DEPOT);

        ArgumentCaptor<Iterable<Delivery>> saved = ArgumentCaptor.forClass(Iterable.class);
        verify(deliveries).saveAll(saved.capture());
        assertThat(saved.getValue()).containsExactly(unanswered);
    }

    private void whenFailuresAre(Delivery... failures) {
        when(deliveries.findByOutcomeNotAndUndoneAtIsNullOrderByCompletedAtDesc(DeliveryOutcome.DELIVERED))
                .thenReturn(List.of(failures));
    }

    private static Delivery failed(String id) {
        return Delivery.record(new Delivery.RecordedDelivery(id, "stop-" + id, "order-" + id, "VEH-X",
                DeliveryOutcome.FAILED, 0, DeliveryReason.STORE_CLOSED, null, null, null, AT, AT,
                "usr-driver", "client-" + id));
    }
}
