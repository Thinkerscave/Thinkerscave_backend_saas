package com.thinkerscave.platform.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import com.thinkerscave.platform.enums.OperationStatus;
import com.thinkerscave.platform.enums.ProvisionStatus;

public record TenantHealthResponse(
        Long tenantId,
        String organizationName,
        String tenantIdentifier,
        String schemaName,
        String databaseVersion,
        String catalogVersion,
        OperationStatus migrationStatus,
        Long storageUsedMb,
        ProvisionStatus provisionStatus,
        String healthStatus,
        Integer healthScore,
        boolean maintenanceMode,
        LocalDateTime lastCheckAt,
        List<String> issues
) {
}
