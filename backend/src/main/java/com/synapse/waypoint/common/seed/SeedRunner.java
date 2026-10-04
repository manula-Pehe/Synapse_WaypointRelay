package com.synapse.waypoint.common.seed;

import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Runs every {@link SeedStep} once, all or nothing. */
@Component
public class SeedRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedRunner.class);

    private final List<SeedStep> steps;
    private final SeedMarker marker;

    SeedRunner(List<SeedStep> steps, SeedMarker marker) {
        this.steps = steps.stream().sorted(Comparator.comparingInt(SeedStep::order)).toList();
        this.marker = marker;
    }

    @Transactional
    public void runOnce() {
        if (marker.isSeeded()) {
            log.info("Seed data already loaded, skipping");
            return;
        }
        for (SeedStep step : steps) {
            log.info("Seeding: {}", step.name());
            step.run();
        }
        marker.markSeeded();
        log.info("Seed data loaded");
    }
}
