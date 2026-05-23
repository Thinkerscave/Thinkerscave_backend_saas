package com.thinkerscave.common.config;

import org.springframework.beans.factory.annotation.Value;
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
 * 
 * In dev profile (H2), multi-tenancy is disabled and all data lives in PUBLIC schema.
 */
@Configuration
public class HibernateMultiTenantConfig {

    private final SchemaMultiTenantConnectionProvider connectionProvider;
    private final DevMultiTenantConnectionProvider devConnectionProvider;
    private final SchemaTenantResolver schemaTenantResolver;

    @Value("${app.multi-tenancy.enabled:true}")
    private boolean multiTenancyEnabled;

    @Value("${spring.jpa.hibernate.ddl-auto:none}")
    private String ddlAuto;

    public HibernateMultiTenantConfig(
            @org.springframework.beans.factory.annotation.Autowired(required = false) SchemaMultiTenantConnectionProvider connectionProvider,
            @org.springframework.beans.factory.annotation.Autowired(required = false) DevMultiTenantConnectionProvider devConnectionProvider,
            SchemaTenantResolver schemaTenantResolver) {
        this.connectionProvider = connectionProvider;
        this.devConnectionProvider = devConnectionProvider;
        this.schemaTenantResolver = schemaTenantResolver;
    }

    /**
     * Defines the EntityManagerFactory bean with multi-tenancy enabled via SCHEMA
     * strategy (production) or disabled (dev).
     */
    @Bean
    @org.springframework.context.annotation.Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
            JpaVendorAdapter jpaVendorAdapter) {
        Map<String, Object> properties = new HashMap<>();

        if (multiTenancyEnabled) {
            // Production: Use schema-based multi-tenancy
            properties.put("hibernate.multiTenancy", "SCHEMA");
            properties.put("hibernate.multi_tenant_connection_provider", connectionProvider);
            properties.put("hibernate.tenant_identifier_resolver", schemaTenantResolver);
            properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        } else {
            // Dev: No multi-tenancy, single schema (H2)
            properties.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        }

        properties.put("hibernate.show_sql", true);
        properties.put("hibernate.format_sql", true);
        properties.put("hibernate.hbm2ddl.auto", ddlAuto);

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
                "com.thinkerscave.common.attendance.domain",
                "com.thinkerscave.common.leave.domain",
                "com.thinkerscave.common.payroll.domain",
                "com.thinkerscave.common.commonModel");
        emf.setJpaVendorAdapter(jpaVendorAdapter);
        emf.setJpaPropertyMap(properties);
        return emf;
    }
}