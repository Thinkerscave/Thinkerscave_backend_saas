package com.thinkerscave.retention.dto;

import com.thinkerscave.retention.RetentionTrigger;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class RetentionPurgeResult {
    String taskKey;
    String label;
    int retentionDays;
    LocalDateTime cutoffAt;
    int deletedCount;
    RetentionTrigger triggerType;
    Long organizationId;
    String actorUsername;
    LocalDateTime ranAt;
    String summary;
}
