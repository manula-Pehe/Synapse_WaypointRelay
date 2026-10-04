package com.synapse.waypoint.core.job;

import java.util.List;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import com.synapse.waypoint.common.time.DemoClock;
import com.synapse.waypoint.core.reference.repository.OutletRepository;

@Configuration
class JobsConfig {

    @Bean
    JobRunner jobRunner(ObjectProvider<DemoClockJob> jobs, JobCompletionLog completionLog, DemoClock clock,
            OutletRepository outlets, PlatformTransactionManager transactionManager) {
        TransactionTemplate perJob = new TransactionTemplate(transactionManager);
        perJob.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        List<DemoClockJob> allJobs = jobs.orderedStream().toList();
        return new JobRunner(allJobs, completionLog, clock, outlets, perJob);
    }

    /** Only active with {@code app.jobs.enabled} (JOBS_ENABLED, default true). */
    @Configuration
    @EnableScheduling
    @ConditionalOnProperty(name = "app.jobs.enabled", havingValue = "true", matchIfMissing = true)
    static class TriggersConfig {

        @Bean
        JobScheduler jobScheduler(JobRunner runner) {
            return new JobScheduler(runner);
        }

        @Bean
        ClockMoveJobTrigger clockMoveJobTrigger(JobRunner runner) {
            return new ClockMoveJobTrigger(runner);
        }
    }
}
