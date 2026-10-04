package com.synapse.waypoint.core.job.hook;

import java.time.LocalDate;

/**
 * Implemented by the driver module. At 2 PM on the run day, failed deliveries whose store has not
 * replied are planned again for the next day. Until an implementation exists the job skips itself.
 */
public interface FailedDeliveryRetry {

    void replanUnansweredFailures(LocalDate runDate, String depot);
}
