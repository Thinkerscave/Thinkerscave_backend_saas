package com.thinkerscave.platform.dto.response;

public record ReleaseSummaryResponse(
        ReleaseResponse currentRelease,
        long totalTenants,
        long upToDate,
        long pendingMigration,
        long failedOrMaintenance
) {
}
