package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.ReleaseStatus;

import java.time.LocalDateTime;

public record ReleaseOverviewResponse(
        Long id,
        String releaseId,
        String releaseVersion,
        String applicationVersion,
        String targetDatabaseVersion,
        String targetCatalogVersion,
        ReleaseStatus status,
        LocalDateTime createdAt,
        LocalDateTime releasedAt,
        String createdBy,
        String releaseNotes,
        long totalTenants,
        long succeeded,
        long failed,
        long pending,
        int progressPercent
) {
}
