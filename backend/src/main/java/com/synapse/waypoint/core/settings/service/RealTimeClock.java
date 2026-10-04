package com.synapse.waypoint.core.settings.service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.error.DomainException;
import com.synapse.waypoint.common.error.ErrorCode;
import com.synapse.waypoint.common.time.DemoClock;

/** Wall time and the next Colombo delivery day for non-demo deployments. */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "false", matchIfMissing = true)
class RealTimeClock implements AdjustableDemoClock {

    private final Clock wallClock;

    RealTimeClock(Clock wallClock) {
        this.wallClock = wallClock;
    }

    @Override
    public Instant now() {
        return wallClock.instant();
    }

    @Override
    public LocalDate runDate() {
        return LocalDate.now(wallClock.withZone(DemoClock.ZONE)).plusDays(1);
    }

    @Override
    public void moveTo(Instant at) {
        throw new DomainException(ErrorCode.INVALID_STATUS, "The clock cannot be moved outside demo mode.");
    }
}
