package com.thinkerscave.platform.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Per-tenant Flyway preview / validation result. Read-only: no migration is executed.
 */
public record MigrationPreviewResponse(
        Long tenantId,
        String tenantIdentifier,
        String schemaName,
        String currentVersion,
        String targetVersion,
        int pendingCount,
        int appliedCount,
        boolean validationSuccessful,
        String validationMessage,
        List<ScriptStateResponse> pendingScripts,
        List<ValidationCheckResponse> checks,
        boolean maintenanceMode,
        LocalDateTime checkedAt
) {
}
