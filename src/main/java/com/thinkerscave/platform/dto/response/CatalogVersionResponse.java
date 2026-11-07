package com.thinkerscave.platform.dto.response;

import java.time.LocalDateTime;

public record CatalogVersionResponse(
        Long versionNumber,
        String changeType,
        String entityType,
        String entityKey,
        String createdBy,
        LocalDateTime createdOn
) {
}
