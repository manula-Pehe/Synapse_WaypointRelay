package com.synapse.waypoint.core.job;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.synapse.waypoint.core.job.hook.FailedDeliveryRetry;
import com.synapse.waypoint.core.job.hook.ReceiptAutoClose;

/**
 * Which run-day hooks the running application actually has.
 *
 * <p>A job with no implementation skips itself, so this is what decides whether the 2 PM re-plan
 * and the receipt auto-close do anything in a real deployment. The jobs themselves are covered by
 * {@code RunDayHookJobTests}; this asks the different question of which modules have signed up.
 *
 * <p>The jobs are not run here on purpose. The retry job re-plans unanswered failures, and this is
 * a test against the real seeded database - running it would decide failures that other tests then
 * read back.
 */
@SpringBootTest
class RunDayHookWiringTests {

    @Autowired
    ObjectProvider<FailedDeliveryRetry> failedDeliveryRetry;

    @Autowired
    ObjectProvider<ReceiptAutoClose> receiptAutoClose;

    @Test
    void theDriverModuleSuppliesTheFailedDeliveryRetry() {
        // F9: a dispatcher who has not answered by 2 PM has the failure re-planned for tomorrow.
        assertThat(failedDeliveryRetry.getIfAvailable()).isNotNull();
    }

    @Test
    void noModuleSuppliesTheReceiptAutoCloseYet() {
        // Store screens are out of this branch's scope, so this job still skips itself.
        assertThat(receiptAutoClose.getIfAvailable()).isNull();
    }
}
