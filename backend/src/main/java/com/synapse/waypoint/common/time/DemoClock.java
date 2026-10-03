package com.synapse.waypoint.common.time;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

/**
 * The system's notion of "now". The demo can move it forward, so business logic must use this
 * instead of {@code Instant.now()} or {@code LocalDate.now()}.
 */
public interface DemoClock {

    /** Business time zone for the whole system. */
    ZoneId ZONE = ZoneId.of("Asia/Colombo");

    Instant now();

    /** The delivery day currently being ordered for and planned. */
    LocalDate runDate();

    default OffsetDateTime nowLocal() {
        return now().atZone(ZONE).toOffsetDateTime();
    }

    default LocalDate today() {
        return now().atZone(ZONE).toLocalDate();
    }
}
