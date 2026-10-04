package com.synapse.waypoint.core.job;

import java.time.Instant;

import org.springframework.stereotype.Component;

import com.synapse.waypoint.core.settings.entity.AppSetting;
import com.synapse.waypoint.core.settings.repository.AppSettingRepository;

/** Keeps completion in {@code app_settings} under {@code job.<name>.<runDate>.<depot>}. */
@Component
class AppSettingJobCompletionLog implements JobCompletionLog {

    private static final String KEY_PREFIX = "job.";
    private static final int MAX_KEY_LENGTH = 60;

    private final AppSettingRepository settings;

    AppSettingJobCompletionLog(AppSettingRepository settings) {
        this.settings = settings;
    }

    @Override
    public boolean isDone(String jobName, JobRun run) {
        return settings.existsById(keyFor(jobName, run));
    }

    @Override
    public void markDone(String jobName, JobRun run, Instant at) {
        settings.save(new AppSetting(keyFor(jobName, run), at.toString(), at));
    }

    static String keyFor(String jobName, JobRun run) {
        String key = KEY_PREFIX + jobName + "." + run.runDate() + "." + run.depot();
        if (key.length() > MAX_KEY_LENGTH) {
            throw new IllegalArgumentException("Job completion key is too long: " + key);
        }
        return key;
    }
}
