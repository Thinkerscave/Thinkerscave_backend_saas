package com.thinkerscave.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

/**
 * Loads dev seed data AFTER Hibernate has created all entity tables.
 * Active only under the "dev" Spring profile.
 */
@Component
@Profile("dev")
@Slf4j
@RequiredArgsConstructor
public class DevDataInitializer implements ApplicationRunner {

    private final DataSource dataSource;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Loading dev seed data from db/dev/data.sql ...");
        try {
            ResourceDatabasePopulator migrate = new ResourceDatabasePopulator();
            migrate.addScript(new ClassPathResource("db/dev/migrate_customers_simplify.sql"));
            migrate.addScript(new ClassPathResource("db/dev/migrate_customer_contacts_normalize.sql"));
            migrate.addScript(new ClassPathResource("db/dev/migrate_customers_owner_user.sql"));
            migrate.setSeparator(";");
            migrate.setContinueOnError(true);
            migrate.execute(dataSource);
            ensureCustomerOwnerColumn();
            log.info("Customer schema migrations applied (if needed).");

            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("db/dev/data.sql"));
            populator.addScript(new ClassPathResource("db/dev/data-phase3-production-demo.sql"));
            populator.addScript(new ClassPathResource("db/dev/data-phase4-customer-management.sql"));
            populator.setSeparator(";");
            populator.setContinueOnError(true);
            populator.execute(dataSource);
            log.info("Dev seed data loaded.");

            provisionTenantDatabases();

            // Fix password hashes so they match the app's BCrypt encoder.
            // Dev default for all seeded users (including platform superadmin).
            String hash = passwordEncoder.encode("Password@123");
            int updated = jdbcTemplate.update(
                    "UPDATE users SET password = ?, account_locked = FALSE, failed_login_attempts = 0, lock_expiry_at = NULL WHERE password IS NOT NULL",
                    hash);
            log.info("Updated {} user passwords with BCrypt hash and cleared lockouts.", updated);

            jdbcTemplate.update("""
                    INSERT IGNORE INTO roles (id, role_code, role_name, description, role_type, dashboard_code, system_role, active, display_order, created_by, updated_by, version)
                    VALUES (6, 'ROLE_SUPER_ADMIN', 'ThinkersCave Super Admin', 'Platform control tower and tenant administration', 'SUPER_ADMIN', 'PLATFORM', TRUE, TRUE, 0, 'system', 'system', 0)
                    """);
            int superAdminRoles = jdbcTemplate.update("""
                    UPDATE user_roles ur
                    INNER JOIN users u ON u.id = ur.user_id
                    SET ur.role_id = 6
                    WHERE u.username = 'superadmin' AND ur.primary_role = TRUE
                    """);
            log.info("Ensured SUPER_ADMIN role for superadmin ({} row(s) updated).", superAdminRoles);

            int promotionFix = jdbcTemplate.update(
                    "UPDATE promotions SET discount_type = 'FLAT_AMOUNT' WHERE promotion_code = 'ODISHA_LAUNCH'");
            promotionFix += jdbcTemplate.update(
                    "UPDATE promotions SET discount_type = 'PERCENTAGE' WHERE promotion_code = 'SUMMER2026'");
            promotionFix += jdbcTemplate.update(
                    "DELETE FROM promotions WHERE discount_type IS NULL OR discount_type = ''");
            if (promotionFix > 0) {
                log.info("Normalized promotion seed data ({} row change(s)).", promotionFix);
            }

                int followUpFix = jdbcTemplate.update(
                    "UPDATE inquiry_follow_up SET follow_up_type = 'CALL' WHERE follow_up_type IS NULL OR TRIM(follow_up_type) = ''");
                followUpFix += jdbcTemplate.update(
                    "UPDATE inquiry SET last_follow_up_type = 'CALL' WHERE last_follow_up_type IS NOT NULL AND TRIM(last_follow_up_type) = ''");
                if (followUpFix > 0) {
                log.info("Normalized follow-up enum seed data ({} row change(s)).", followUpFix);
                }

            // Ensure tenant_registry.active is TRUE (seed INSERT may miss column)
            jdbcTemplate.update("UPDATE tenant_registry SET active = TRUE WHERE active IS NULL OR active = FALSE");

            // Ensure staff.staff_type is never empty/null
            jdbcTemplate.update("UPDATE staff SET staff_type = 'TEACHING' WHERE staff_type IS NULL OR TRIM(staff_type) = ''");

            // Ensure notice_audience.audience_type is never empty/null
            jdbcTemplate.update("UPDATE notice_audience SET audience_type = 'ALL' WHERE audience_type IS NULL OR TRIM(audience_type) = ''");

            // Ensure application_admission.status is never empty/null
            jdbcTemplate.update("UPDATE application_admission SET status = 'SUBMITTED' WHERE status IS NULL OR TRIM(status) = ''");

            // Ensure inquiry.status is never empty/null
            jdbcTemplate.update("UPDATE inquiry SET status = 'NEW' WHERE status IS NULL OR TRIM(status) = ''");

            // Ensure promotion discount_type defaults
            jdbcTemplate.update("UPDATE promotions SET discount_type = 'FLAT_AMOUNT' WHERE discount_type IS NULL OR TRIM(discount_type) = ''");

            log.info("Enum/data consistency fixes applied.");
        } catch (Exception e) {
            log.warn("Dev seed data load issue: {}", e.getMessage());
        }
    }

    /**
     * Provisions each tenant database:
     *   1. CREATE DATABASE IF NOT EXISTS
     *   2. Copy all table structures from thinkerscave_dev using CREATE TABLE ... LIKE
     *   3. Load tenant-specific seed SQL (db/tenant/seed_{slug}.sql)
     *   4. BCrypt-rehash placeholder passwords in tenant schema
     */
    private void provisionTenantDatabases() {
        List<Map<String, Object>> tenants = new ArrayList<>();
        try {
            tenants = jdbcTemplate.queryForList(
                    "SELECT schema_name, tenant_identifier FROM tenant_registry " +
                    "WHERE schema_name IS NOT NULL AND TRIM(schema_name) <> ''");
        } catch (Exception e) {
            log.warn("Could not read tenant_registry: {}", e.getMessage());
        }

        if (tenants.isEmpty()) {
            log.warn("No tenant schemas in tenant_registry; skipping tenant provisioning.");
            return;
        }

        // Collect all platform table names once
        List<String> platformTables = new ArrayList<>();
        try {
            platformTables = jdbcTemplate.queryForList(
                    "SELECT table_name FROM information_schema.tables " +
                    "WHERE table_schema = 'thinkerscave_dev' AND table_type = 'BASE TABLE'",
                    String.class);
        } catch (Exception e) {
            log.warn("Could not read platform table list: {}", e.getMessage());
        }

        String bcryptHash = passwordEncoder.encode("Password@123");

        for (Map<String, Object> tenant : tenants) {
            String schema   = (String) tenant.get("schema_name");
            String tenantId = (String) tenant.get("tenant_identifier");
            String slug     = tenantId.replace("-", "_");
            String seedFile = "db/tenant/seed_" + slug + ".sql";

            try {
                // 1. Create tenant database
                jdbcTemplate.execute(
                        "CREATE DATABASE IF NOT EXISTS `" + schema +
                        "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                log.info("Tenant database ready: {}", schema);

                // 2. Copy table structures from platform schema
                for (String table : platformTables) {
                    try {
                        jdbcTemplate.execute(
                                "CREATE TABLE IF NOT EXISTS `" + schema + "`.`" + table +
                                "` LIKE `thinkerscave_dev`.`" + table + "`");
                    } catch (Exception te) {
                        log.debug("Table copy skipped {}.{}: {}", schema, table, te.getMessage());
                    }
                }

                ensureCustomerOwnerColumn(schema);
                log.info("Table structures copied to {}", schema);

                // 3. Load tenant seed data into the tenant schema
                ClassPathResource seedResource = new ClassPathResource(seedFile);
                if (seedResource.exists()) {
                    try (Connection conn = dataSource.getConnection()) {
                        conn.setCatalog(schema);
                        SingleConnectionDataSource tenantDs =
                                new SingleConnectionDataSource(conn, true);
                        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
                        populator.addScript(seedResource);
                        populator.setSeparator(";");
                        populator.setContinueOnError(true);
                        populator.execute(tenantDs);

                        // 4. BCrypt-rehash placeholder passwords
                        JdbcTemplate tenantJdbc = new JdbcTemplate(tenantDs);
                        tenantJdbc.update(
                                "UPDATE users SET password = ?, account_locked = FALSE, failed_login_attempts = 0, lock_expiry_at = NULL WHERE password IS NOT NULL",
                                bcryptHash);
                        log.info("Seed data and passwords loaded for tenant: {}", schema);
                    }
                } else {
                    log.warn("Seed file not found for tenant '{}' at path: {}", tenantId, seedFile);
                }
            } catch (Exception e) {
                log.warn("Could not provision tenant database {}: {}", schema, e.getMessage());
            }
        }
    }

    private void ensureCustomerOwnerColumn() {
        ensureCustomerOwnerColumn(null);
    }

    private void ensureCustomerOwnerColumn(String schema) {
        String tableRef = (schema == null || schema.isBlank()) ? "customers" : ("`" + schema + "`.`customers`");
        try (Connection connection = dataSource.getConnection()) {
            String catalog = (schema == null || schema.isBlank()) ? connection.getCatalog() : schema;
            boolean exists;
            try (ResultSet columns = connection.getMetaData().getColumns(catalog, null, "customers", "owner_user_id")) {
                exists = columns.next();
            }

            if (!exists) {
                jdbcTemplate.execute("ALTER TABLE " + tableRef + " ADD COLUMN owner_user_id BIGINT NULL");
                log.info("Applied fallback migration: {}.owner_user_id added", tableRef);
            }

            try {
                if (schema == null || schema.isBlank()) {
                    jdbcTemplate.execute("CREATE INDEX idx_customer_owner ON customers (owner_user_id)");
                } else {
                    jdbcTemplate.execute("CREATE INDEX idx_customer_owner ON " + tableRef + " (owner_user_id)");
                }
            } catch (Exception ignored) {
                // Ignore duplicate-index or unsupported-if-exists differences across engines.
            }
        } catch (Exception ex) {
            log.warn("Could not ensure {} owner_user_id migration: {}", tableRef, ex.getMessage());
        }
    }
}
