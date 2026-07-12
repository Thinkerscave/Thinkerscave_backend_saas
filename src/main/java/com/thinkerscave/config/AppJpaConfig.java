package com.thinkerscave.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * JPA configuration for the modular package structure.
 *
 * <p>Scans all domain entities and Spring Data repositories under
 * {@code com.thinkerscave}. Wires Hibernate's DATABASE multi-tenancy providers
 * so every Hibernate session is routed to the correct MySQL catalog based on the
 * tenant identifier resolved from the current HTTP request context.
 *
 * <p>Schema routing is handled by {@link TenantConnectionProvider}.
 * Tenant identification is handled by {@link TenantIdentifierResolver}.
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

    /**
     * Registers the DATABASE multi-tenancy providers with Hibernate.
     *
     * <p>Spring Boot's JPA autoconfiguration picks this up automatically and
     * passes the properties into the {@code LocalContainerEntityManagerFactoryBean}
     * before it is built — no custom {@code EntityManagerFactory} definition needed.
     */
    @Bean
    public HibernatePropertiesCustomizer hibernateMultiTenancyCustomizer(
            MultiTenantConnectionProvider<String> connectionProvider,
            CurrentTenantIdentifierResolver<String> tenantIdentifierResolver) {
        return hibernateProperties -> {
            hibernateProperties.put("hibernate.multi_tenant_connection_provider", connectionProvider);
            hibernateProperties.put("hibernate.tenant_identifier_resolver", tenantIdentifierResolver);
        };
    }
}

