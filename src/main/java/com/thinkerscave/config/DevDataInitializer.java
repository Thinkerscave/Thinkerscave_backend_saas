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
            populator.setSeparator(";");
            populator.setContinueOnError(true);
            populator.execute(dataSource);
            log.info("Dev seed data loaded.");

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
        } catch (Exception e) {
            log.warn("Dev seed data load issue: {}", e.getMessage());
        }
    }
}
