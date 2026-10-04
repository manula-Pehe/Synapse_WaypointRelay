package com.synapse.waypoint.core.settings.service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.settings.dto.SettingsResponse;
import com.synapse.waypoint.core.settings.event.DemoClockMoved;

@Service
class DefaultSettingsService implements SettingsService {

    private final AdjustableDemoClock clock;
    private final ApplicationEventPublisher events;

    DefaultSettingsService(AdjustableDemoClock clock, ApplicationEventPublisher events) {
        this.clock = clock;
        this.events = events;
    }

    @Override
    public SettingsResponse current() {
        return new SettingsResponse(
                clock.runDate(), clock.nowLocal().truncatedTo(ChronoUnit.SECONDS), DemoClock.ZONE.getId());
    }

    @Override
    @Transactional
    public SettingsResponse moveClock(OffsetDateTime at) {
        clock.moveTo(at.toInstant());
        events.publishEvent(new DemoClockMoved(at.toInstant()));
        return current();
    }
}
