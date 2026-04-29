package com.thinkerscave.common.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;

@Configuration
@OpenAPIDefinition(info = @Info(contact = @Contact(name = "ThinkersCave", email = "support@thinkerscave.com", url = "https://thinkerscave.com"), description = "OpenApi documentation for ThinkersCave SaaS Backend", title = "ThinkersCave SaaS API", version = "1.0"), servers = {
        @Server(description = "Local ENV", url = "http://localhost:8181"),
        @Server(description = "Production ENV", url = "https://api.thinkerscave.com")
}, security = { @SecurityRequirement(name = "bearerAuth") } // Global JWT – all endpoints show lock icon
)
@SecurityScheme(name = "bearerAuth", description = "JWT Bearer Token Authentication", scheme = "bearer", type = SecuritySchemeType.HTTP, bearerFormat = "JWT", in = SecuritySchemeIn.HEADER)
public class OpenApiConfig {

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("thinkerscave-saas")
                .pathsToMatch("/api/**")
                .addOperationCustomizer(tenantHeaderCustomizer())
                .build();
    }

    @Bean
    public OperationCustomizer tenantHeaderCustomizer() {
        return (operation, handlerMethod) -> {
            Parameter tenantHeader = new Parameter()
                    .in("header")
                    .name("X-Tenant-ID")
                    .description(
                            "Tenant/Schema identifier (e.g., mumbai_school, delhi_school). Required for multi-tenant isolation.")
                    .required(false) // Optional because auto-detected from JWT or subdomain
                    .example("mumbai_school");

            operation.addParametersItem(tenantHeader);
            return operation;
        };
    }
}
