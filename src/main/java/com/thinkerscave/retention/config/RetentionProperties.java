package com.thinkerscave.retention.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Central, configurable archival/retention settings.
 *
 * <p>Drives the shared retention scheduler and the effective retention window
 * for every registered {@link com.thinkerscave.retention.RetentionTask}. A task
 * can be registered purely in configuration — no code change — as long as it is
 * a Spring bean; its own default {@code retention-days} is used unless a config
 * entry overrides it here.
 *
 * <pre>
 * app.retention.enabled=true
 * app.retention.cron=0 30 2 * * *
 * app.retention.tasks.LOGIN_HISTORY.retention-days=30
 * app.retention.tasks.LOGIN_HISTORY.enabled=true
 * </pre>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.retention")
public class RetentionProperties {

    /** Master switch for the nightly archival job and manual purge runs. */
    private boolean enabled = true;

    /** Spring cron expression for the nightly archival job. Defaults to 02:30 daily. */
    private String cron = "0 30 2 * * *";

    /** Per-task overrides keyed by task key (case-insensitive). */
    private Map<String, TaskConfig> tasks = new LinkedHashMap<>();

    public TaskConfig configOf(String taskKey) {
        if (taskKey == null || tasks.isEmpty()) {
            return null;
        }
        String upper = taskKey.toUpperCase(Locale.ROOT);
        TaskConfig exact = tasks.get(upper);
        if (exact != null) {
            return exact;
        }
        return tasks.entrySet().stream()
                .filter(e -> upper.equals(String.valueOf(e.getKey()).toUpperCase(Locale.ROOT)))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
    }

    /** Per-task overrides. All fields optional — absent values fall back to the task defaults. */
    @Getter
    @Setter
    public static class TaskConfig {
        /** Overrides {@code RetentionTask.retentionDays()}, e.g. 30 or 180 days. */
        private Integer retentionDays;
        /** Allows disabling a single dataset without removing its bean. */
        private Boolean enabled;
    }
}