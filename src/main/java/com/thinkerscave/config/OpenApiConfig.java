package com.thinkerscave.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                contact = @Contact(name = "ThinkersCave", email = "support@thinkerscave.com",
                        url = "https://thinkerscave.com"),
                description = "ThinkersCave SaaS Platform — enterprise education management API",
                title = "ThinkersCave SaaS API",
                version = "2.0"
        ),
        servers = {
                @Server(description = "Local / Dev", url = "http://localhost:8181"),
                @Server(description = "Production",  url = "https://api.thinkerscave.com")
        },
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT Bearer Token",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
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
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("X-Tenant-ID")
                    .description("Tenant/schema identifier (auto-detected from JWT or subdomain).")
                    .required(false)
                    .example("mumbai_school"));
            operation.addParametersItem(new Parameter()
                    .in("header")
                    .name("X-Organization-ID")
                    .description("Organization ID override (super-admin only).")
                    .required(false));
            return operation;
        };
    }
}
