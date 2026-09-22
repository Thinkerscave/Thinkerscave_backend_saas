package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.OperationStatus;
import java.time.LocalDateTime;

public record MigrationExecutionResponse(
        String executionId,
        Long tenantId,
        String releaseId,
        String currentDatabaseVersion,
        String targetDatabaseVersion,
        String migrationVersion,
        String description,
        OperationStatus status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        LocalDateTime failedAt,
        String failedMigrationVersion,
        String errorMessage,
        int retryCount
) {}
