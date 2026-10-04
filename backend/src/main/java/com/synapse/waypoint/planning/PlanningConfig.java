package com.synapse.waypoint.planning;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.synapse.waypoint.planning.engine.PlanningEngine;

@Configuration
class PlanningConfig {

    @Bean
    PlanningEngine planningEngine() {
        return new PlanningEngine();
    }
}
