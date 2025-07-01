package com.thinkerscave.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("ThinkersCave SaaS Backend API")
                        .version("1.0.0")
                        .description("""
                        ThinkersCave SaaS is a scalable multi-tenant backend platform designed to support organizations 
                        in managing their internal users, roles, and operational structure efficiently. Built using Spring Boot 
                        with schema-based multi-tenancy, it ensures robust isolation and modularity for each tenant.

                        Core Modules:
                        - Organization Management: Register and onboard new organizations. Each gets its own schema.
                        - User Management: Create and manage users per organization with role-based access control.
                        - Role Management: Define, assign, and enforce roles and permissions across modules.

                        Upcoming Features:
                        - Staff Management: Employee records, designations, and assignments per organization.
                        - Roll Management: A domain-specific module for managing organizational roll structures.

                        Multi-Tenant Design:
                        - Each organization (tenant) is isolated via a dedicated PostgreSQL schema.
                        - A request header 'X-Tenant-ID' is used to dynamically switch schema per request.

                        Security:
                        - Tenant context managed per request using a thread-local strategy.
                        - Future releases will support JWT-based authentication and granular permissions.

                        Notes:
                        - Built using Spring Boot, Spring Data JPA, and Hibernate
                        - RESTful APIs ready for Angular/React-based frontends
                        - Ideal for B2B SaaS applications with multi-organization support
                        """)
                        .contact(new Contact()
                                .name("ThinkersCave Dev Team")
                                .email("support@thinkerscave.com")
                                .url("https://www.thinkerscave.com"))
                        .license(new License()
                                .name("Apache 2.0 License")
                                .url("http://www.apache.org/licenses/LICENSE-2.0.html"))
                );
    }

}
