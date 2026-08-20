package com.thinkerscave.platform.service;

import com.thinkerscave.platform.entity.TenantRegistry;
import com.thinkerscave.platform.repository.TenantRegistryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reads live tenant-schema metrics into {@link TenantRegistry} so Super Admin
 * details pages never hardcode usage. A daily job refreshes every tenant;
 * opening an organization also refreshes if the snapshot is older than 24h.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantInsightService {

    private static final String ADMIN_SQL = """
            SELECT u.display_name, u.email, u.mobile_number
            FROM "%s"."users" u
            JOIN "%s"."user_roles" ur ON ur.user_id = u.id AND ur.active = true
            JOIN "%s"."roles" r ON r.id = ur.role_id
            WHERE r.role_code = 'ROLE_ADMIN'
            ORDER BY u.id
            LIMIT 1
            """;

    private static final String ADMIN_ID_SQL = """
            SELECT u.id
            FROM "%s"."users" u
            JOIN "%s"."user_roles" ur ON ur.user_id = u.id AND ur.active = true
            JOIN "%s"."roles" r ON r.id = ur.role_id
            WHERE r.role_code = 'ROLE_ADMIN'
            ORDER BY u.id
            LIMIT 1
            """;

    private static final String UPDATE_ADMIN_SQL = """
            UPDATE "%s"."users"
            SET first_name = ?, last_name = ?, display_name = ?, email = ?, mobile_number = ?
            WHERE id = ?
            """;

    private final TenantRegistryRepository tenantRegistryRepository;
    private final JdbcTemplate jdbcTemplate;

    public record OrgAdmin(String fullName, String email, String mobile) {}

    public OrgAdmin findOrgAdmin(String schemaName) {
        if (!StringUtils.hasText(schemaName) || !schemaExists(schemaName)) {
            return null;
        }
        try {
            return jdbcTemplate.query(
                    ADMIN_SQL.formatted(schemaName, schemaName, schemaName),
                    rs -> rs.next()
                            ? new OrgAdmin(rs.getString(1), rs.getString(2), rs.getString(3))
                            : null);
        } catch (Exception ex) {
            log.debug("Could not read org admin from schema {}: {}", schemaName, ex.getMessage());
            return null;
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public TenantRegistry refreshUsage(TenantRegistry tenant) {
        if (tenant == null || !StringUtils.hasText(tenant.getSchemaName())) {
            return tenant;
        }
        String schema = tenant.getSchemaName();
        tenant.setStudentCount((int) countRows(schema, "student"));
        tenant.setStaffCount((int) countRows(schema, "staff"));
        tenant.setBranchCount((int) countRows(schema, "branch"));
        tenant.setClassCount((int) countRows(schema, "academic_class"));
        tenant.setSectionCount((int) countRows(schema, "academic_section"));
        long sizeMb = schemaSizeMb(schema);
        if (sizeMb >= 0) {
            tenant.setDatabaseSizeMb(sizeMb);
            tenant.setStorageUsedMb(sizeMb);
        }
        tenant.setUsageRefreshedAt(LocalDateTime.now());
        return tenantRegistryRepository.save(tenant);
    }

    @Transactional
    public TenantRegistry refreshIfStale(TenantRegistry tenant) {
        if (tenant == null) {
            return null;
        }
        LocalDateTime refreshed = tenant.getUsageRefreshedAt();
        if (refreshed == null || refreshed.isBefore(LocalDateTime.now().minusHours(24))) {
            return refreshUsage(tenant);
        }
        return tenant;
    }

    /**
     * Keep the first ROLE_ADMIN user in the tenant schema in sync with the
     * organization admin contact captured on the platform org record.
     */
    public void updateOrgAdmin(String schemaName, String fullName, String email, String mobile) {
        if (!StringUtils.hasText(schemaName) || !schemaExists(schemaName) || !tableExists(schemaName, "users")) {
            return;
        }
        try {
            Long userId = jdbcTemplate.query(
                    ADMIN_ID_SQL.formatted(schemaName, schemaName, schemaName),
                    rs -> rs.next() ? rs.getLong(1) : null);
            if (userId == null) {
                return;
            }
            String[] names = splitName(fullName);
            jdbcTemplate.update(
                    UPDATE_ADMIN_SQL.formatted(schemaName),
                    names[0],
                    names[1],
                    fullName,
                    email,
                    mobile,
                    userId);
        } catch (Exception ex) {
            log.warn("Could not update org admin in schema {}: {}", schemaName, ex.getMessage());
        }
    }

    @Scheduled(cron = "0 20 2 * * *")
    @Transactional
    public void refreshAllTenantsDaily() {
        List<TenantRegistry> tenants = tenantRegistryRepository.findAll();
        log.info("Refreshing usage snapshots for {} tenants", tenants.size());
        for (TenantRegistry tenant : tenants) {
            try {
                refreshUsage(tenant);
            } catch (Exception ex) {
                log.warn("Usage refresh failed for tenant {}: {}", tenant.getTenantIdentifier(), ex.getMessage());
            }
        }
    }

    private long countRows(String schema, String table) {
        if (!tableExists(schema, table)) {
            return 0;
        }
        try {
            Long count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM \"" + schema + "\".\"" + table + "\"",
                    Long.class);
            return count == null ? 0 : count;
        } catch (Exception ex) {
            log.debug("Count failed for {}.{}: {}", schema, table, ex.getMessage());
            return 0;
        }
    }

    private long schemaSizeMb(String schema) {
        try {
            Long bytes = jdbcTemplate.queryForObject(
                    """
                    SELECT COALESCE(SUM(pg_total_relation_size(
                            quote_ident(schemaname) || '.' || quote_ident(tablename))), 0)
                    FROM pg_tables WHERE schemaname = ?
                    """,
                    Long.class,
                    schema);
            if (bytes == null) {
                return -1;
            }
            return Math.max(0, bytes / (1024 * 1024));
        } catch (Exception ex) {
            log.debug("Schema size failed for {}: {}", schema, ex.getMessage());
            return -1;
        }
    }

    private boolean schemaExists(String schema) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name = ?",
                Integer.class,
                schema);
        return count != null && count > 0;
    }

    private boolean tableExists(String schema, String table) {
        Integer count = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*) FROM information_schema.tables
                WHERE table_schema = ? AND table_name = ?
                """,
                Integer.class,
                schema,
                table);
        return count != null && count > 0;
    }

    private String[] splitName(String fullName) {
        String trimmed = fullName == null ? "" : fullName.trim();
        if (!StringUtils.hasText(trimmed)) {
            return new String[] {"Admin", "User"};
        }
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            return new String[] {trimmed, trimmed};
        }
        String last = trimmed.substring(space + 1).trim();
        return new String[] {trimmed.substring(0, space), StringUtils.hasText(last) ? last : trimmed.substring(0, space)};
    }
}
