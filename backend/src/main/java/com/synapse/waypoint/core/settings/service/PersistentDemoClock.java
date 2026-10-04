package com.synapse.waypoint.core.settings.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.synapse.waypoint.core.settings.entity.AppSetting;
import com.synapse.waypoint.core.settings.repository.AppSettingRepository;

/**
 * Demo time = real time + a stored offset, so the demo keeps ticking after it is moved.
 * The offset and run date live in {@code app_settings} and survive restarts.
 */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
class PersistentDemoClock implements AdjustableDemoClock {

    static final String OFFSET_KEY = "clock_offset_seconds";
    static final String RUN_DATE_KEY = "run_date";

    private final Clock wallClock;
    private final AppSettingRepository settings;
    private final DemoProperties defaults;
    private final AtomicReference<State> state = new AtomicReference<>();

    PersistentDemoClock(Clock wallClock, AppSettingRepository settings, DemoProperties defaults) {
        this.wallClock = wallClock;
        this.settings = settings;
        this.defaults = defaults;
    }

    @Override
    public Instant now() {
        return wallClock.instant().plus(state().offset());
    }

    @Override
    public LocalDate runDate() {
        return state().runDate();
    }

    @Override
    public void moveTo(Instant at) {
        Duration offset = Duration.between(wallClock.instant(), at);
        save(OFFSET_KEY, String.valueOf(offset.getSeconds()));
        state.set(new State(offset, state().runDate()));
    }

    private State state() {
        State current = state.get();
        if (current == null) {
            current = load();
            state.set(current);
        }
        return current;
    }

    private State load() {
        Duration offset = settings.findById(OFFSET_KEY)
                .map(setting -> Duration.ofSeconds(Long.parseLong(setting.getValue())))
                .orElseGet(this::initialOffset);
        LocalDate runDate = settings.findById(RUN_DATE_KEY)
                .map(setting -> LocalDate.parse(setting.getValue()))
                .orElseGet(this::initialRunDate);
        return new State(offset, runDate);
    }

    private Duration initialOffset() {
        Duration offset = Duration.between(wallClock.instant(), defaults.start().toInstant());
        save(OFFSET_KEY, String.valueOf(offset.getSeconds()));
        return offset;
    }

    private LocalDate initialRunDate() {
        save(RUN_DATE_KEY, defaults.runDate().toString());
        return defaults.runDate();
    }

    private void save(String key, String value) {
        Instant at = wallClock.instant();
        AppSetting setting = settings.findById(key)
                .map(existing -> {
                    existing.update(value, at);
                    return existing;
                })
                .orElseGet(() -> new AppSetting(key, value, at));
        settings.save(setting);
    }

    private record State(Duration offset, LocalDate runDate) {
    }
}
