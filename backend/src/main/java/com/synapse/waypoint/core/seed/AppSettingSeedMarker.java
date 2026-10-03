package com.synapse.waypoint.core.seed;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.common.seed.SeedMarker;
import com.synapse.waypoint.core.settings.entity.AppSetting;
import com.synapse.waypoint.core.settings.repository.AppSettingRepository;

/** Records completion of the seed in {@code app_settings} under {@value #KEY}. */
@Component
class AppSettingSeedMarker implements SeedMarker {

    static final String KEY = "seeded_at";

    private final AppSettingRepository settings;
    private final Clock wallClock;

    AppSettingSeedMarker(AppSettingRepository settings, Clock wallClock) {
        this.settings = settings;
        this.wallClock = wallClock;
    }

    @Override
    public boolean isSeeded() {
        return settings.existsById(KEY);
    }

    @Override
    public void markSeeded() {
        Instant now = wallClock.instant();
        settings.save(new AppSetting(KEY, now.toString(), now));
    }
}
