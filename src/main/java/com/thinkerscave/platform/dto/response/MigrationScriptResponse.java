package com.thinkerscave.platform.dto.response;

/**
 * A packaged Flyway script plus how many tenant schemas have applied it.
 */
public record MigrationScriptResponse(
        String version,
        String description,
        String script,
        String location,
        long appliedTenantCount,
        long pendingTenantCount,
        long totalTenants
) {
}
