package com.thinkerscave.platform.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Read-only release readiness validation. Producing this response never executes a migration,
 * never deploys an application build and never writes Flyway history.
 */
public record ReleaseValidationResponse(
        boolean valid,
        String releaseId,
        String targetDatabaseVersion,
        String latestAvailableDatabaseVersion,
        String targetCatalogVersion,
        String currentCatalogVersion,
        long eligibleTenants,
        List<ValidationCheckResponse> checks,
        LocalDateTime checkedAt
) {
}
