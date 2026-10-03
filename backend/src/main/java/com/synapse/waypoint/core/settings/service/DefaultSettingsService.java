package com.synapse.waypoint.core.settings.service;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.settings.dto.SettingsResponse;

@Service
class DefaultSettingsService implements SettingsService {

    private final AdjustableDemoClock clock;

    DefaultSettingsService(AdjustableDemoClock clock) {
        this.clock = clock;
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
        return current();
    }
}
