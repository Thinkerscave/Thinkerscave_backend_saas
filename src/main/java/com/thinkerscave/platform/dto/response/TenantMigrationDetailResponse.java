package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.OperationStatus;

import java.time.LocalDateTime;
import java.util.List;

public record TenantMigrationDetailResponse(
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
        String healthStatus,
        Integer healthScore,
        List<String> healthIssues,
        String currentFlywayVersion,
        List<ScriptStateResponse> pendingScripts,
        int appliedScriptCount,
        List<MigrationExecutionResponse> migrationHistory,
        List<CatalogSyncExecutionResponse> catalogSyncHistory,
        LocalDateTime lastMigrationAt,
        LocalDateTime lastHealthCheckAt,
        boolean active
) {
}
