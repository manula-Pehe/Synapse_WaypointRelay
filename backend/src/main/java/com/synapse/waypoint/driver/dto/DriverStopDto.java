package com.synapse.waypoint.driver.dto;

import java.time.OffsetDateTime;

/**
 * One stop on a driver's run, shaped for the phone (docs/api.md §7, R2/R3).
 *
 * The window is flattened into wall-clock {@code HH:mm} and the ETA into a time of day, because the
 * driver reads both at a glance in a cab and neither is a date on the screen.
 */
public record DriverStopDto(
        String id,
        int seq,
        String orderId,
        String orderRef,
        String outletId,
        String outletName,
        String district,
        String dockType,
        String windowOpen,
        String windowClose,
        int units,
        String temp,
        OffsetDateTime arriveFrom,
        OffsetDateTime arriveTo,
        /** Minutes early against the window opening; negative means running late. */
        int earlyByMinutes,
        /** True once the order has moved to another vehicle - the driver keeps working (R2c). */
        boolean reassigned) {
}