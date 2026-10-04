package com.synapse.waypoint.core.settings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.synapse.waypoint.common.error.DomainException;

class RealTimeClockTests {

    @Test
    void usesWallTimeAndTheNextColomboDeliveryDay() {
        Instant now = Instant.parse("2026-10-03T20:00:00Z");
        RealTimeClock clock = new RealTimeClock(Clock.fixed(now, ZoneOffset.UTC));

        assertThat(clock.now()).isEqualTo(now);
        assertThat(clock.runDate()).isEqualTo(LocalDate.parse("2026-10-05"));
    }

    @Test
    void rejectsClockChangesOutsideDemoMode() {
        RealTimeClock clock = new RealTimeClock(Clock.systemUTC());
        assertThatThrownBy(() -> clock.moveTo(Instant.now())).isInstanceOf(DomainException.class);
    }
}
