package com.synapse.waypoint.core.settings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.synapse.waypoint.core.settings.entity.AppSetting;
import com.synapse.waypoint.core.settings.repository.AppSettingRepository;

class PersistentDemoClockTests {

    private static final OffsetDateTime DEMO_START = OffsetDateTime.parse("2026-09-30T14:00:00+05:30");
    private static final LocalDate RUN_DATE = LocalDate.parse("2026-10-01");

    private final Map<String, AppSetting> stored = new HashMap<>();
    private final MutableClock wallClock = new MutableClock(Instant.parse("2026-10-03T04:00:00Z"));
    private PersistentDemoClock clock;

    @BeforeEach
    void setUp() {
        AppSettingRepository repository = mock(AppSettingRepository.class);
        when(repository.findById(any())).thenAnswer(call -> Optional.ofNullable(stored.get(call.<String>getArgument(0))));
        when(repository.save(any())).thenAnswer(call -> {
            AppSetting setting = call.getArgument(0);
            stored.put(setting.getKey(), setting);
            return setting;
        });
        clock = new PersistentDemoClock(wallClock, repository, new DemoProperties(DEMO_START, RUN_DATE));
    }

    @Test
    void shouldStartAtTheConfiguredDemoTimeWhenNothingIsStored() {
        assertThat(clock.now()).isEqualTo(DEMO_START.toInstant());
        assertThat(clock.runDate()).isEqualTo(RUN_DATE);
        assertThat(stored).containsKeys(PersistentDemoClock.OFFSET_KEY, PersistentDemoClock.RUN_DATE_KEY);
    }

    @Test
    void shouldKeepTickingFromTheMovedTime() {
        Instant movedTo = OffsetDateTime.parse("2026-09-30T16:05:00+05:30").toInstant();

        clock.moveTo(movedTo);
        wallClock.advance(Duration.ofMinutes(10));

        assertThat(clock.now()).isEqualTo(movedTo.plus(Duration.ofMinutes(10)));
    }

    @Test
    void shouldReportLocalTimeInSriLanka() {
        assertThat(clock.nowLocal()).isEqualTo(DEMO_START);
        assertThat(clock.today()).isEqualTo(LocalDate.parse("2026-09-30"));
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant start) {
            this.instant = start;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
