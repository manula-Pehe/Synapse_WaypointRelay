package com.synapse.waypoint.core.job;

import java.time.LocalTime;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import com.synapse.waypoint.core.job.hook.FailedDeliveryRetry;

/** 2 PM on the run day: failed deliveries with no store reply are re-planned for the next day. */
@Component
class FailedDeliveryRetryJob extends RunDayHookJob<FailedDeliveryRetry> {

    static final String NAME = "failed-delivery-retry";

    private static final LocalTime RETRY_TIME = LocalTime.of(14, 0);

    FailedDeliveryRetryJob(ObjectProvider<FailedDeliveryRetry> retry) {
        super(retry, RETRY_TIME);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    protected void invoke(FailedDeliveryRetry retry, JobRun run) {
        retry.replanUnansweredFailures(run.runDate(), run.depot());
    }
}
