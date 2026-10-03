package com.synapse.waypoint.core.job;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = { "app.jobs.enabled=true", "app.jobs.tick-interval=1h" })
class JobsEnabledTests {

    @Autowired ApplicationContext context;

    @Test
    void shouldStartTheSchedulerAndTheClockTrigger() {
        assertThat(context.getBeansOfType(JobScheduler.class)).hasSize(1);
        assertThat(context.getBeansOfType(ClockMoveJobTrigger.class)).hasSize(1);
    }
}
