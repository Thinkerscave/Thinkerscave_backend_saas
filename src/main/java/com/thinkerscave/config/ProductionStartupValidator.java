package com.thinkerscave.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;

/**
 * Fails fast when production profile is active without required secrets,
 * or when an ambiguous profile set would run production with insecure defaults.
 */
@Slf4j
@Component
public class ProductionStartupValidator implements ApplicationRunner {

    private final Environment environment;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    public ProductionStartupValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        boolean alsoDev = Arrays.asList(environment.getActiveProfiles()).contains("dev");
        if (!prod) {
            return;
        }
        if (alsoDev) {
            throw new IllegalStateException(
                    "Invalid profile combination: 'prod' and 'dev' must not be active together");
        }

        requireEnv("DB_URL", environment.getProperty("spring.datasource.url"));
        requireEnv("DB_USERNAME", environment.getProperty("spring.datasource.username"));
        requireEnv("DB_PASSWORD", environment.getProperty("spring.datasource.password"));
        requireEnv("JWT_SECRET", jwtSecret);
        requireEnv("ALLOWED_ORIGINS", environment.getProperty("app.cors.allowed-origins"));

        if (jwtSecret != null && (jwtSecret.contains("dev_secret") || jwtSecret.contains("testing_only")
                || jwtSecret.contains("replace_with"))) {
            throw new IllegalStateException("Production JWT_SECRET must not use a development placeholder value");
        }

        log.info("Production startup validation passed (profiles={})",
                Arrays.toString(environment.getActiveProfiles()));
    }

    private void requireEnv(String name, String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                    "Missing required production configuration: " + name
                            + ". Set environment variable before starting with spring.profiles.active=prod");
        }
    }
}
