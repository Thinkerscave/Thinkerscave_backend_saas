//package com.thinkerscave.common.orgm.controller;
//
//
////import com.thinkerscave.common.config.TenantContext;
//import com.thinkerscave.common.orgm.service.SchemaInitializer;
//import com.thinkerscave.common.orgm.service.SchemaService;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.Parameter;
//import io.swagger.v3.oas.annotations.enums.ParameterIn;
//import io.swagger.v3.oas.annotations.media.ArraySchema;
//import io.swagger.v3.oas.annotations.media.Content;
//import io.swagger.v3.oas.annotations.media.ExampleObject;
//import io.swagger.v3.oas.annotations.media.Schema;
//import io.swagger.v3.oas.annotations.responses.ApiResponse;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.web.bind.annotation.*;
//
//import java.util.Collections;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//import java.util.stream.Collectors;
//
//@RestController
//@RequestMapping("/api/schema")
//@Tag(name = "Schema Management", description = "APIs for managing database schemas (tenants)")
//public class SchemaController {
//
//    @Autowired
//    private JdbcTemplate jdbcTemplate;
//
//    @Autowired
//    private SchemaService schemaService;
//
//    @Autowired
//    private SchemaInitializer schemaInitializer;
//
//    @Operation(
//            summary = "Get all schemas",
//            description = "Returns a list of all schemas excluding PostgreSQL system schemas."
//    )
//    @ApiResponse(
//            responseCode = "200",
//            description = "List of schemas",
//            content = @Content(
//                    mediaType = "application/json",
//                    array = @ArraySchema(schema = @Schema(implementation = String.class)),
//                    examples = @ExampleObject(value = """
//                                ["demo_org", "tenant1", "tenant2"]
//                            """)
//            )
//    )
//    @GetMapping
//    public List<String> getAllSchemas() {
//        return jdbcTemplate.queryForList("SELECT schema_name FROM information_schema.schemata", String.class)
//                .stream()
//                .filter(name -> !name.startsWith("pg_") && !name.equals("information_schema"))
//                .collect(Collectors.toList());
//    }
//
//    @Operation(
//            summary = "Create and initialize a schema",
//            description = "Creates a new schema and initializes default tables.",
//            parameters = {
//                    @Parameter(
//                            name = "name",
//                            description = "The name of the schema to be created",
//                            required = true,
//                            in = ParameterIn.QUERY,
//                            example = "demo_org"
//                    )
//            },
//            responses = {
//                    @ApiResponse(
//                            responseCode = "200",
//                            description = "Schema created successfully",
//                            content = @Content(
//                                    mediaType = "text/plain",
//                                    examples = @ExampleObject(value = "Schema and tables created for: demo_org")
//                            )
//                    ),
//                    @ApiResponse(
//                            responseCode = "400",
//                            description = "Schema already exists",
//                            content = @Content(
//                                    mediaType = "text/plain",
//                                    examples = @ExampleObject(value = "Schema already exists")
//                            )
//                    )
//            }
//    )
//    @PostMapping("/create-and-init")
//    public ResponseEntity<String> createAndInit(@RequestParam(required = false) String name) {
//        // Default to 'public' if name is null or blank
//        String schemaName = (name == null || name.isBlank()) ? "public" : name;
//
//        if (schemaService.schemaExists(schemaName)) {
//            return ResponseEntity.badRequest().body("Schema already exists");
//        }
//
//        schemaService.createSchema(schemaName);
//        schemaInitializer.createTablesForSchema(schemaName);
//        return ResponseEntity.ok("Schema and tables created for: " + schemaName);
//    }
//
//
//    @PostMapping("/init")
//    public ResponseEntity<String> init(@RequestParam(required = false) String name) {
//        // Default to 'public' if name is null or blank
//        String schemaName = (name == null || name.isBlank()) ? "public" : name;
//        schemaInitializer.createTablesForSchema(schemaName);
//        return ResponseEntity.ok("Tables created for: " + schemaName);
//    }
//
////    @Operation(
////            summary = "Get current tenant/schema",
////            description = "Returns the schema (tenant) set for the current request based on the tenant context.",
////            parameters = {
////                    @Parameter(
////                            name = "X-Tenant-ID",
////                            description = "Tenant ID or schema name passed via request header",
////                            in = ParameterIn.HEADER,
////                            required = false,
////                            example = "demo_org"
////                    )
////            },
////            responses = {
////                    @ApiResponse(responseCode = "200", description = "Tenant context response",
////                            content = @Content(
////                                    mediaType = "text/plain",
////                                    examples = {
////                                            @ExampleObject(name = "With schema", value = "Current connected schema: demo_org"),
////                                            @ExampleObject(name = "Without schema", value = "No schema (tenant) set for this request.")
////                                    }
////                            )
////                    )
////            }
////    )
////    @GetMapping("/current")
////    public ResponseEntity<String> getCurrentTenant() {
////        String currentSchema = TenantContext.getTenant();
////        if (currentSchema == null) {
////            return ResponseEntity.ok("No schema (tenant) set for this request.");
////        }
////        return ResponseEntity.ok("Current connected schema: " + currentSchema);
////    }
//
//
////    @GetMapping("/check")
////    public ResponseEntity<?> checkSchemaTables() {
////        // Get tenant (schema) name from context
////        String schemaName = TenantContext.getTenant();
////
////        // Default to "public" if null or blank
////        schemaName = (schemaName == null || schemaName.isBlank()) ? "public" : schemaName;
////
////        try {
////            List<String> missingTables = schemaService.getMissingTables(schemaName);
////
////            if (missingTables.isEmpty()) {
////                return ResponseEntity.ok(Map.of(
////                        "schema", schemaName,
////                        "allTablesExist", true,
////                        "message", "All required tables exist."
////                ));
////            } else {
////                return ResponseEntity.ok(Map.of(
////                        "schema", schemaName,
////                        "allTablesExist", false,
////                        "missingTables", missingTables,
////                        "message", "Some tables are missing."
////                ));
////            }
////
////        } catch (IllegalArgumentException e) {
////            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
////                    "schema", schemaName,
////                    "error", e.getMessage()
////            ));
////
////        } catch (Exception e) {
////            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
////                    "schema", schemaName,
////                    "error", "An error occurred: " + e.getMessage()
////            ));
////        }
////    }
//
//
////    @PostMapping("/validate-tables")
////    public ResponseEntity<?> validateAndCreateTables(@RequestBody List<String> tableNames) {
////        // Get tenant (schema) name from context
////        String schemaName = TenantContext.getTenant();
////
////        // Default to "public" if null or blank
////        schemaName = (schemaName == null || schemaName.isBlank()) ? "public" : schemaName;
////
////        List<String> missingTables = schemaService.getMissingTables(schemaName);
////
////        if (!missingTables.isEmpty()) {
////            schemaInitializer.createMissingTablesInSchema(schemaName, tableNames);
////            Map<String, Object> response = new HashMap<>();
////            response.put("message", "Created missing tables in schema: " + schemaName);
////            response.put("createdTables", missingTables);
////            return ResponseEntity.status(HttpStatus.CREATED).body(response);
////        }
////
////        return ResponseEntity.ok(Collections.singletonMap(
////                "message", "All requested tables already exist in schema: " + schemaName
////        ));
////    }
//
//
//
//
//}
//
