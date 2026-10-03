package com.synapse.waypoint.common.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Loads the demo data when the application starts (switch off with {@code app.seed.enabled=false}).
 * Any failure, such as a missing dataset file, stops the start-up so the problem is not missed.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
class SeedOnStartup implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedOnStartup.class);

    private final SeedRunner runner;

    SeedOnStartup(SeedRunner runner) {
        this.runner = runner;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            runner.runOnce();
        } catch (RuntimeException e) {
            log.error("Seeding failed, stopping start-up: {}", e.getMessage());
            throw e;
        }
    }
}
