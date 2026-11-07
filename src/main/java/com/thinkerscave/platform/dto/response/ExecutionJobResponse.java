package com.thinkerscave.platform.dto.response;

import java.time.LocalDateTime;

/**
 * Unified execution job row across database migrations, catalog synchronization and provisioning.
 */
public record ExecutionJobResponse(
        String jobId,
        String jobType,
        Long tenantId,
        String tenantIdentifier,
        String organizationName,
        String releaseId,
        String status,
        int attempt,
        String targetVersion,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String errorMessage
) {
}
