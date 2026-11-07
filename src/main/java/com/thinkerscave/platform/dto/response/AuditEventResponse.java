package com.thinkerscave.platform.dto.response;

import java.time.LocalDateTime;

public record AuditEventResponse(
        Long id,
        String eventType,
        String actor,
        String tenantIdentifier,
        String entityType,
        String entityId,
        String status,
        String targetVersion,
        String details,
        String errorMessage,
        LocalDateTime createdAt
) {
}
