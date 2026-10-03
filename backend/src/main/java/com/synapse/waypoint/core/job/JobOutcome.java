package com.synapse.waypoint.core.job;

/** What a job did. A skipped job counts as done: there was nothing (left) to do for that run. */
public record JobOutcome(Status status, String detail) {

    public enum Status { COMPLETED, SKIPPED }

    public static JobOutcome completed(String detail) {
        return new JobOutcome(Status.COMPLETED, detail);
    }

    public static JobOutcome skipped(String reason) {
        return new JobOutcome(Status.SKIPPED, reason);
    }

    public boolean isSkipped() {
        return status == Status.SKIPPED;
    }
}
