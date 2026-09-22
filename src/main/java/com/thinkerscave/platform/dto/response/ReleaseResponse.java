package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.ReleaseStatus;

import java.time.LocalDateTime;

public record ReleaseResponse(
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
        String releaseNotes
) {
}
