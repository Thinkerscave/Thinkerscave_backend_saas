package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.OperationStatus;
import java.time.LocalDateTime;

public record TenantReleaseOperationResponse(
        Long tenantId,
        Long organizationId,
        String organizationName,
        String tenantIdentifier,
        String schemaName,
        String applicationVersion,
        String databaseVersion,
        String targetDatabaseVersion,
        String catalogVersion,
        String targetCatalogVersion,
        OperationStatus migrationStatus,
        OperationStatus catalogSyncStatus,
        boolean maintenanceMode,
        String maintenanceReason,
        LocalDateTime maintenanceStartedAt,
        String healthStatus,
        LocalDateTime lastMigrationAt,
        LocalDateTime lastHealthCheckAt
) {}
