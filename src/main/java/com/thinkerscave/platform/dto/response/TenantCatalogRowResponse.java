package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.OperationStatus;

import java.time.LocalDateTime;

public record TenantCatalogRowResponse(
        Long tenantId,
        String organizationName,
        String tenantIdentifier,
        String schemaName,
        String catalogVersion,
        String targetCatalogVersion,
        OperationStatus syncStatus,
        LocalDateTime lastSyncedAt,
        String errorMessage
) {
}
