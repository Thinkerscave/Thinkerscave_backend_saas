package com.thinkerscave.common.config;

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
 * This avoids the race condition where Spring's sql.init runs before DDL.
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
            populator.setContinueOnError(false);
            populator.execute(dataSource);
            log.info("Dev seed data loaded successfully.");

            // Fix password hashes using the app's actual BCryptPasswordEncoder
            String encodedPassword = passwordEncoder.encode("Password@123");
            int updated = jdbcTemplate.update("UPDATE users SET password = ? WHERE password IS NOT NULL", encodedPassword);
            log.info("Updated {} user password(s) with proper BCrypt hash.", updated);
        } catch (Exception e) {
            log.error("Failed to load dev seed data: {}", e.getMessage(), e);
        }
    }
}
