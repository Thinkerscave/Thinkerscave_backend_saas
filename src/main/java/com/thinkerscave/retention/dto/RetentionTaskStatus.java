package com.thinkerscave.retention.dto;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.List;

@Value
@Builder
public class RetentionTaskStatus {
    String taskKey;
    String label;
    String description;
    int retentionDays;
    /** Whether this dataset is currently picked up by the nightly archival job. */
    boolean enabled;
    /** Raw Spring cron expression, e.g. {@code 0 30 2 * * *}. */
    String scheduleCron;
    /** Human-readable schedule description. */
    String schedule;
    RetentionPurgeResult lastRun;
    List<RetentionPurgeResult> recentRuns;
    LocalDateTime nextScheduledHint;
}