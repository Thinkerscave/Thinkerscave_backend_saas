package com.thinkerscave.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration for the new modular package structure.
 * Scans all domain entities and Spring Data repositories under
 * {@code com.thinkerscave} EXCEPT the decommissioned {@code common} package.
 */
@Configuration
@EnableJpaRepositories(basePackages = {
        "com.thinkerscave.access.repository",
        "com.thinkerscave.academics.repository",
        "com.thinkerscave.admission.repository",
        "com.thinkerscave.attendance.repository",
        "com.thinkerscave.audit.repository",
        "com.thinkerscave.communication.repository",
        "com.thinkerscave.document.repository",
        "com.thinkerscave.platform.repository",
        "com.thinkerscave.security.repository",
        "com.thinkerscave.shared.repository",
        "com.thinkerscave.staff.repository",
        "com.thinkerscave.student.repository"
})
@EntityScan(basePackages = {
        "com.thinkerscave.access.entity",
        "com.thinkerscave.academics.entity",
        "com.thinkerscave.admission.entity",
        "com.thinkerscave.attendance.entity",
        "com.thinkerscave.audit.entity",
        "com.thinkerscave.communication.entity",
        "com.thinkerscave.document.entity",
        "com.thinkerscave.platform.entity",
        "com.thinkerscave.security.entity",
        "com.thinkerscave.shared.entity",
        "com.thinkerscave.staff.entity",
        "com.thinkerscave.student.entity"
})
public class AppJpaConfig {
}
