package com.thinkerscave.platform.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Catalog synchronization state. Catalog sync is an independent lifecycle from application
 * releases and from database migrations.
 */
public record CatalogOverviewResponse(
        CatalogSyncStatusResponse status,
        String latestCatalogVersion,
        List<CatalogVersionResponse> versions,
        List<TenantCatalogRowResponse> tenants,
        LocalDateTime checkedAt
) {
}
