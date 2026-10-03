package com.synapse.waypoint.common.time;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Real wall clock, injected so tests can replace it. Only {@link DemoClock} implementations use it.
 */
@Configuration
public class TimeConfig {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
