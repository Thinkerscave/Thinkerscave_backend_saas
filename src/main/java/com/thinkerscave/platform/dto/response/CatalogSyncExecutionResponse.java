package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.OperationStatus;
import java.time.LocalDateTime;

public record CatalogSyncExecutionResponse(
        String executionId,
        Long tenantId,
        String catalogVersion,
        String targetCatalogVersion,
        OperationStatus status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String errorMessage,
        int retryCount
) {}
