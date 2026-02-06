package com.thinkerscave.common.orgm.controller;

import com.thinkerscave.common.orgm.service.SchemaInitializer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for testing multi-tenancy schema operations.
 * This controller provides endpoints for creating and managing tenant schemas.
 */
@CrossOrigin("*")
@RestController
@RequestMapping("/api/tenant")
@Tag(name = "Tenant Management", description = "APIs for managing tenant schemas")
@RequiredArgsConstructor
@Slf4j
public class TenantController {

    private final SchemaInitializer schemaInitializer;
    private final PasswordEncoder passwordEncoder;

    /**
     * Create a new tenant schema.
     * This will create the schema and Hibernate will auto-create tables.
     */
    @PostMapping("/create/{schemaName}")
    @Operation(summary = "Create a new tenant schema")
    public ResponseEntity<Map<String, Object>> createTenant(@PathVariable String schemaName) {
        Map<String, Object> response = new HashMap<>();
        response.put("schemaName", schemaName);

        try {
            boolean created = schemaInitializer.createSchemaIfNotExists(schemaName);
            if (created) {
                response.put("status", "CREATED");
                response.put("message", "Schema '" + schemaName
                        + "' created successfully. Tables will be auto-created by Hibernate on first access.");
            } else {
                response.put("status", "EXISTS");
                response.put("message", "Schema '" + schemaName + "' already exists.");
            }
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to create schema: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Check if a schema exists.
     */
    @GetMapping("/exists/{schemaName}")
    @Operation(summary = "Check if tenant schema exists")
    public ResponseEntity<Map<String, Object>> checkSchemaExists(@PathVariable String schemaName) {
        Map<String, Object> response = new HashMap<>();
        response.put("schemaName", schemaName);

        try {
            boolean exists = schemaInitializer.schemaExists(schemaName);
            response.put("exists", exists);
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * List all tenant schemas (excluding system schemas).
     */
    @GetMapping("/list")
    @Operation(summary = "List all tenant schemas")
    public ResponseEntity<Map<String, Object>> listSchemas() {
        Map<String, Object> response = new HashMap<>();

        try {
            List<String> schemas = schemaInitializer.listTenantSchemas();
            response.put("schemas", schemas);
            response.put("count", schemas.size());
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Seeds a test user for a tenant.
     * WARNING: Only for development/testing.
     */
    @PostMapping("/seed-user/{schemaName}")
    @Operation(summary = "Seed admin user for tenant")
    public ResponseEntity<Map<String, Object>> seedUser(@PathVariable String schemaName, @RequestParam String username,
            @RequestParam String password) {
        log.debug("Seeding user for schema: {}", schemaName);
        log.debug("Username: {}", username);
        log.debug("Password Hash Received: {}", password);

        Map<String, Object> response = new HashMap<>();
        response.put("schemaName", schemaName);
        response.put("username", username);

        try {
            // Hash the password before storing
            String hashedPassword = passwordEncoder.encode(password);
            log.debug("Password hashed successfully");

            schemaInitializer.seedTenantUser(schemaName, username, hashedPassword);
            response.put("status", "SEEDED");
            response.put("message", "User created successfully.");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to seed user: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/debug-user/{schemaName}")
    public ResponseEntity<Map<String, Object>> debugUser(@PathVariable String schemaName,
            @RequestParam String username) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> users = schemaInitializer.debugListUsers(schemaName, username);
            response.put("users", users);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    @GetMapping("/verify-hash")
    public boolean verifyHash(@RequestParam String raw, @RequestParam String hash) {
        return passwordEncoder.matches(raw, hash);
    }

    /**
     * Drop a tenant schema (USE WITH CAUTION - this deletes all data).
     */
    @DeleteMapping("/drop/{schemaName}")
    @Operation(summary = "Drop a tenant schema (DESTRUCTIVE)")
    public ResponseEntity<Map<String, Object>> dropSchema(@PathVariable String schemaName) {
        Map<String, Object> response = new HashMap<>();
        response.put("schemaName", schemaName);

        // Prevent dropping critical schemas
        if ("public".equals(schemaName) || schemaName.startsWith("pg_") || "information_schema".equals(schemaName)) {
            response.put("status", "REJECTED");
            response.put("message", "Cannot drop system schema: " + schemaName);
            return ResponseEntity.badRequest().body(response);
        }

        try {
            if (!schemaInitializer.schemaExists(schemaName)) {
                response.put("status", "NOT_FOUND");
                response.put("message", "Schema '" + schemaName + "' does not exist.");
                return ResponseEntity.ok(response);
            }

            schemaInitializer.dropSchema(schemaName);
            response.put("status", "DROPPED");
            response.put("message", "Schema '" + schemaName + "' has been dropped.");
            return ResponseEntity.ok(response);
        } catch (SQLException e) {
            response.put("status", "ERROR");
            response.put("message", "Failed to drop schema: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
