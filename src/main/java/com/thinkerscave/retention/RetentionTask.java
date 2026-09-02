package com.thinkerscave.retention;

/**
 * One archivable dataset in the shared retention process.
 *
 * <p>To add a new table to the archival pipeline, implement this interface and
 * register it as a Spring bean. The shared {@code RetentionServiceImpl} purges
 * rows generically from {@link #table()} using {@link #dateColumn()} as the
 * age cut-off — no changes to the service are needed. The effective retention
 * window can be tuned centrally via {@code app.retention.tasks.<KEY>.retention-days}.
 */
public interface RetentionTask {

    /** Unique, stable key. Used as the map key in {@code app.retention.tasks.*}. */
    String key();

    String label();

    String description();

    /** Default retention window in days. Configurable via {@code app.retention.tasks.<KEY>.retention-days}. */
    int retentionDays();

    /** Physical table purged by this task. Never qualifies the schema (added by the service). */
    String table();

    /** Timestamp column used for the "older than N days" cut-off. */
    String dateColumn();

    /** Whether the dataset can be purged for a single organization (vs. all tenants at once). */
    boolean organizationScoped();

    /**
     * Optional SQL fragment appended when a single organization is requested.
     * Must reference the org id through a single {@code ?} placeholder, e.g.
     * {@code user_id IN (SELECT id FROM users WHERE organization_id = ?)}.
     */
    default String organizationClause() {
        return "";
    }
}