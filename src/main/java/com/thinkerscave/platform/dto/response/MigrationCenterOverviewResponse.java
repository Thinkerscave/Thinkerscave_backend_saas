package com.thinkerscave.platform.dto.response;

import java.time.LocalDateTime;

/**
 * Consolidated Migration Center KPI snapshot. Every value is derived from persisted platform
 * state (releases, executions, catalog versions, tenant registry) - never from static data.
 */
public record MigrationCenterOverviewResponse(
        String latestAvailableDatabaseVersion,
        String latestAvailableCatalogVersion,
        Long currentReleaseId,
        String currentReleaseVersion,
        String currentReleaseStatus,
        String currentApplicationVersion,
        String targetDatabaseVersion,
        String targetCatalogVersion,
        long totalTenants,
        long databaseCurrent,
        long databasePending,
        long databaseFailed,
        long catalogSynced,
        long catalogPending,
        long catalogFailed,
        long maintenanceTenants,
        long activeJobs,
        long failedJobs,
        long totalReleases,
        long migrationScripts,
        LocalDateTime checkedAt
) {
}
