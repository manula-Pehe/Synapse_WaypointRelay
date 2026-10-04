package com.synapse.waypoint.store;

import java.time.Instant;

/** Whether the truck's phone is out of signal, so a quiet screen is not mistaken for a stalled delivery. */
record DriverStatusView(boolean offline, Instant lastSyncAt) {
}
