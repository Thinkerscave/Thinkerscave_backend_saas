package com.thinkerscave.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**

 - Configures Hibernate to use schema-based multi-tenancy
 - by wiring a custom connection provider and tenant resolver.
 */

/**
 * HibernateMultiTenantConfig sets up schema-based multi-tenancy in a Spring Boot application.
 *
 * It defines the entity manager factory bean with specific Hibernate properties to support
 * multi-tenant behavior, where each tenant has its own schema.
 */

@Configuration
public class HibernateMultiTenantConfig {

    // Custom implementation to provide database connections for different tenants (schemas)
    private final SchemaMultiTenantConnectionProvider connectionProvider;

    // Custom implementation that determines the current tenant identifier (i.e., schema name)
    private final SchemaTenantResolver schemaTenantResolver;

    // Constructor-based dependency injection for the custom connection provider and tenant resolver
    public HibernateMultiTenantConfig(SchemaMultiTenantConnectionProvider connectionProvider,
                                      SchemaTenantResolver schemaTenantResolver) {
        this.connectionProvider = connectionProvider;
        this.schemaTenantResolver = schemaTenantResolver;
    }

    /**
     * Defines the EntityManagerFactory bean with multi-tenancy enabled via SCHEMA strategy.
     *
     * - Uses PostgreSQL dialect
     * - Enables SQL logging (show_sql, format_sql)
     * - Registers custom tenant resolver and connection provider
     * - Scans the specified packages for entity classes
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
                                                                       JpaVendorAdapter jpaVendorAdapter) {
        Map<String, Object> properties = new HashMap<>();

        // Use schema-based multi-tenancy strategy
        properties.put("hibernate.multiTenancy", "SCHEMA");

        // Plug in custom multi-tenant connection provider and tenant identifier resolver
        properties.put("hibernate.multi_tenant_connection_provider", connectionProvider);
        properties.put("hibernate.tenant_identifier_resolver", schemaTenantResolver);

        // Hibernate-specific settings
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.show_sql", true);
        properties.put("hibernate.format_sql", true);

        // Setup EntityManagerFactory with injected settings and package scanning for entities
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan(
                "com.thinkerscave.common.usrm.domain",
                "com.thinkerscave.common.role.domain",
                "com.thinkerscave.common.orgm.domain",
                "com.thinkerscave.common.menum.domain",
                "com.thinkerscave.common.staff.domain"
        );
        emf.setJpaVendorAdapter(jpaVendorAdapter);
        emf.setJpaPropertyMap(properties);
        return emf;
    }
}