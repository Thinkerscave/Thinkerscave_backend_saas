package com.thinkerscave.common.orgm.controller;


import com.thinkerscave.common.orgm.config.TenantContext;
import com.thinkerscave.common.orgm.service.SchemaInitializer;
import com.thinkerscave.common.orgm.service.SchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/schemas")
@Tag(name = "Schema Management", description = "APIs for managing database schemas (tenants)")
public class SchemaController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SchemaInitializer schemaInitializer;

    @Operation(
            summary = "Get all schemas",
            description = "Returns a list of all schemas excluding PostgreSQL system schemas."
    )
    @ApiResponse(
            responseCode = "200",
            description = "List of schemas",
            content = @Content(
                    mediaType = "application/json",
                    array = @ArraySchema(schema = @Schema(implementation = String.class)),
                    examples = @ExampleObject(value = """
                ["demo_org", "tenant1", "tenant2"]
            """)
            )
    )
    @GetMapping
    public List<String> getAllSchemas() {
        return jdbcTemplate.queryForList("SELECT schema_name FROM information_schema.schemata", String.class)
                .stream()
                .filter(name -> !name.startsWith("pg_") && !name.equals("information_schema"))
                .collect(Collectors.toList());
    }

    @Operation(
            summary = "Create and initialize a schema",
            description = "Creates a new schema and initializes default tables.",
            parameters = {
                    @Parameter(
                            name = "name",
                            description = "The name of the schema to be created",
                            required = true,
                            in = ParameterIn.QUERY,
                            example = "demo_org"
                    )
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Schema created successfully",
                            content = @Content(
                                    mediaType = "text/plain",
                                    examples = @ExampleObject(value = "Schema and tables created for: demo_org")
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Schema already exists",
                            content = @Content(
                                    mediaType = "text/plain",
                                    examples = @ExampleObject(value = "Schema already exists")
                            )
                    )
            }
    )
    @PostMapping("/create-and-init")
    public ResponseEntity<String> createAndInit(@RequestParam String name) {
        if (schemaService.schemaExists(name)) {
            return ResponseEntity.badRequest().body("Schema already exists");
        }

        schemaService.createSchema(name);
        schemaInitializer.createTablesForSchema(name);
        return ResponseEntity.ok("Schema and tables created for: " + name);
    }

    @Operation(
            summary = "Get current tenant/schema",
            description = "Returns the schema (tenant) set for the current request based on the tenant context.",
            parameters = {
                    @Parameter(
                            name = "X-Tenant-ID",
                            description = "Tenant ID or schema name passed via request header",
                            in = ParameterIn.HEADER,
                            required = false,
                            example = "demo_org"
                    )
            },
            responses = {
                    @ApiResponse(responseCode = "200", description = "Tenant context response",
                            content = @Content(
                                    mediaType = "text/plain",
                                    examples = {
                                            @ExampleObject(name = "With schema", value = "Current connected schema: demo_org"),
                                            @ExampleObject(name = "Without schema", value = "No schema (tenant) set for this request.")
                                    }
                            )
                    )
            }
    )
    @GetMapping("/current")
    public ResponseEntity<String> getCurrentTenant() {
        String currentSchema = TenantContext.getTenant();
        if (currentSchema == null) {
            return ResponseEntity.ok("No schema (tenant) set for this request.");
        }
        return ResponseEntity.ok("Current connected schema: " + currentSchema);
    }
}

