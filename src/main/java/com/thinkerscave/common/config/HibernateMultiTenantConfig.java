package com.thinkerscave.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * HibernateMultiTenantConfig sets up schema-based multi-tenancy in a Spring
 * Boot application.
 *
 * It defines the entity manager factory bean with specific Hibernate properties
 * to support
 * multi-tenant behavior, where each tenant has its own schema.
 */
@Configuration
public class HibernateMultiTenantConfig {

    private final SchemaMultiTenantConnectionProvider connectionProvider;
    private final SchemaTenantResolver schemaTenantResolver;

    public HibernateMultiTenantConfig(SchemaMultiTenantConnectionProvider connectionProvider,
            SchemaTenantResolver schemaTenantResolver) {
        this.connectionProvider = connectionProvider;
        this.schemaTenantResolver = schemaTenantResolver;
    }

    /**
     * Defines the EntityManagerFactory bean with multi-tenancy enabled via SCHEMA
     * strategy.
     */
    @Bean
    @org.springframework.context.annotation.Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
            JpaVendorAdapter jpaVendorAdapter) {
        Map<String, Object> properties = new HashMap<>();

        // Use schema-based multi-tenancy strategy
        properties.put("hibernate.multiTenancy", "SCHEMA");

        // Plug in custom multi-tenant connection provider and tenant identifier
        // resolver
        properties.put("hibernate.multi_tenant_connection_provider", connectionProvider);
        properties.put("hibernate.tenant_identifier_resolver", schemaTenantResolver);

        // Hibernate-specific settings
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.show_sql", true);
        properties.put("hibernate.format_sql", true);

        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan(
                "com.thinkerscave.common.usrm.domain",
                "com.thinkerscave.common.role.domain",
                "com.thinkerscave.common.orgm.domain",
                "com.thinkerscave.common.menum.domain",
                "com.thinkerscave.common.staff.domain",
                "com.thinkerscave.common.admission.domain",
                "com.thinkerscave.common.student.domain",
                "com.thinkerscave.common.course.domain",
                "com.thinkerscave.common.commonModel");
        emf.setJpaVendorAdapter(jpaVendorAdapter);
        emf.setJpaPropertyMap(properties);
        return emf;
    }
}