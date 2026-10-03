package com.synapse.waypoint.core.job;

import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.synapse.waypoint.core.settings.event.DemoClockMoved;

/**
 * Runs due jobs as soon as the demo clock has moved, so they have fired when the move returns.
 * Jobs open their own transactions, so they are not tied to the committed move.
 */
class ClockMoveJobTrigger {

    private final JobRunner runner;

    ClockMoveJobTrigger(JobRunner runner) {
        this.runner = runner;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    void onClockMoved(DemoClockMoved event) {
        runner.tick();
    }
}
