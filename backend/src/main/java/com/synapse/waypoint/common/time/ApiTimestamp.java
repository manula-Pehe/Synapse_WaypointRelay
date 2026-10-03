package com.synapse.waypoint.common.time;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/** How an instant is shown in API responses: Sri Lanka offset, whole seconds (same as GET /api/settings). */
public final class ApiTimestamp {

    private ApiTimestamp() {
    }

    /** The instant in Sri Lanka time truncated to seconds, or null when there is none. */
    public static OffsetDateTime of(Instant instant) {
        if (instant == null) {
            return null;
        }
        return instant.atZone(DemoClock.ZONE).toOffsetDateTime().truncatedTo(ChronoUnit.SECONDS);
    }
}
