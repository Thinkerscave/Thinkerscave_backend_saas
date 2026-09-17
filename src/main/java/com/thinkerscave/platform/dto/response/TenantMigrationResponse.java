package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.OperationStatus;

import java.time.LocalDateTime;

public record TenantMigrationResponse(
        Long id,
        Long releaseId,
        Long tenantId,
        String tenantIdentifier,
        String fromVersion,
        String targetVersion,
        String observedVersion,
        OperationStatus status,
        Integer attempt,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String errorMessage
) {
}
