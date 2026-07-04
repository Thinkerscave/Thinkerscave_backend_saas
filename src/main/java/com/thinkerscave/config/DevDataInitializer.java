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
import java.util.ArrayList;
import java.util.List;

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
            ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
            populator.addScript(new ClassPathResource("db/dev/data.sql"));
            populator.addScript(new ClassPathResource("db/dev/data-phase3-production-demo.sql"));
            populator.addScript(new ClassPathResource("db/dev/data-phase4-customer-management.sql"));
            populator.setSeparator(";");
            populator.setContinueOnError(true);
            populator.execute(dataSource);
            log.info("Dev seed data loaded.");

            provisionTenantDatabases();

            // Fix password hashes so they match the app's BCrypt encoder
            String hash = passwordEncoder.encode("Password@123");
            int updated = jdbcTemplate.update(
                    "UPDATE users SET password = ? WHERE password IS NOT NULL", hash);
            log.info("Updated {} user passwords with BCrypt hash.", updated);

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
        } catch (Exception e) {
            log.warn("Dev seed data load issue: {}", e.getMessage());
        }
    }

    /**
     * Creates MySQL databases matching production schema-per-tenant names.
     * Data remains in thinkerscave_dev (row-level org isolation) for local dev;
     * these databases mirror the production tenant_registry.schema_name entries.
     */
    private void provisionTenantDatabases() {
        List<String> schemas = new ArrayList<>();
        try {
            schemas = jdbcTemplate.query(
                    "SELECT DISTINCT schema_name FROM tenant_registry WHERE schema_name IS NOT NULL AND TRIM(schema_name) <> ''",
                    (rs, rowNum) -> rs.getString("schema_name"));
        } catch (Exception e) {
            log.warn("Could not read tenant_registry for schema provisioning: {}", e.getMessage());
        }

        if (schemas.isEmpty()) {
            log.warn("No tenant schemas found in tenant_registry; skipping tenant database provisioning.");
            return;
        }

        for (String schema : schemas) {
            try {
                jdbcTemplate.execute(
                        "CREATE DATABASE IF NOT EXISTS `" + schema + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                log.info("Tenant database ready: {}", schema);
            } catch (Exception e) {
                log.warn("Could not create tenant database {}: {}", schema, e.getMessage());
            }
        }
    }
}
