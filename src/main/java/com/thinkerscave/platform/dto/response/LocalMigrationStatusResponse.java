package com.thinkerscave.platform.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Local development migration readiness. The local workflow is default-disabled and refuses to
 * run against any non-loopback datasource.
 */
public record LocalMigrationStatusResponse(
        boolean enabled,
        boolean loopbackDatasource,
        String datasourceHost,
        String datasourceDatabase,
        String activeProfiles,
        boolean flywayAvailable,
        String currentVersion,
        String latestAvailableVersion,
        int pendingCount,
        List<ScriptStateResponse> pendingScripts,
        boolean validationSuccessful,
        String validationMessage,
        List<String> blockers,
        LocalDateTime checkedAt
) {
}
