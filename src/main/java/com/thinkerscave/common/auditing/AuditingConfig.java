package com.thinkerscave.common.auditing;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables Spring Data JPA auditing.
 * AuditorAwareImpl is a @Component — Spring auto-names it "auditorAwareImpl".
 * We reference that bean name here so createdBy / lastModifiedBy are populated.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class AuditingConfig {
    // AuditorAwareImpl is registered automatically via @Component — no @Bean needed
    // here
}
