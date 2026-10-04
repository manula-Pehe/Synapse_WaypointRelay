package com.synapse.waypoint.driver.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.core.job.hook.FailedDeliveryRetry;
import com.synapse.waypoint.core.order.entity.DeliveryOutcome;
import com.synapse.waypoint.driver.entity.Delivery;
import com.synapse.waypoint.driver.entity.FailedDeliveryDecision;
import com.synapse.waypoint.driver.repository.DeliveryRepository;

/**
 * The 2 PM fallback for failures nobody answered (docs/api.md §8, F9).
 *
 * The rule is deliberately narrow: a failure is re-planned for tomorrow only when neither the store
 * nor dispatch said anything. A dispatcher who already chose "try later today" must not be overruled
 * by a job, so a recorded decision of any kind stops the replan.
 */
@Service
@Transactional
public class DriverFailedDeliveryRetry implements FailedDeliveryRetry {

    private static final Logger log = LoggerFactory.getLogger(DriverFailedDeliveryRetry.class);

    private final DeliveryRepository deliveries;

    public DriverFailedDeliveryRetry(DeliveryRepository deliveries) {
        this.deliveries = deliveries;
    }

    @Override
    @Transactional(readOnly = true)
    public void replanUnansweredFailures(LocalDate runDate, String depot) {
        List<Delivery> unanswered = deliveries
                .findByOutcomeNotAndUndoneAtIsNullOrderByCompletedAtDesc(DeliveryOutcome.DELIVERED).stream()
                .filter(delivery -> delivery.getDecision() == null)
                .filter(delivery -> delivery.getStoreChoice() == null)
                .toList();

        if (unanswered.isEmpty()) {
            return;
        }
        // Recorded so the dispatcher sees the same decision the job applied, and re-running is a no-op.
        Instant decidedAt = runDate.atTime(14, 0).atZone(ZoneOffset.UTC).toInstant();
        unanswered.forEach(delivery -> delivery.decide(
                FailedDeliveryDecision.REPLAN_TOMORROW, "system:2pm-retry", decidedAt));
        deliveries.saveAll(unanswered);

        log.info("Re-planned {} unanswered failed deliveries for {}", unanswered.size(), runDate);
    }
}