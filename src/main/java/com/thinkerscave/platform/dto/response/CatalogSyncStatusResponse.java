package com.thinkerscave.platform.dto.response;

import java.time.LocalDateTime;

public record CatalogSyncStatusResponse(
        long totalTenants,
        long synchronizedTenants,
        long pendingTenants,
        long failedTenants,
        String targetCatalogVersion,
        LocalDateTime checkedAt
) {}
