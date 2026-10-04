package com.synapse.waypoint.driver.dto;

/**
 * A vehicle problem as the phone reports it (docs/api.md §7, R9).
 *
 * {@code tripId} and {@code note} are optional: a breakdown can happen with no run going, and a
 * driver with a fault on the road may have no hands free to type.
 */
public record ReportProblemRequest(
        String tripId,
        String kind,
        Boolean canDrive,
        Double fridgeTempC,
        String note,
        /** The phone's own id, so a queued report retried after a dropped signal is stored once. */
        String clientId) {
}