package com.synapse.waypoint.core.settings.event;

import java.time.Instant;

/** Published after the demo clock has been moved, so time-driven work can catch up. */
public record DemoClockMoved(Instant movedTo) {
}
