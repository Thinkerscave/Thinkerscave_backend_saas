package com.thinkerscave.common.orgm.controller;


import com.thinkerscave.common.orgm.config.TenantContext;
import com.thinkerscave.common.orgm.dto.UserDTO;
import com.thinkerscave.common.orgm.service.SchemaInitializer;
import com.thinkerscave.common.orgm.service.SchemaService;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/schema")
public class SchemaController {

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SchemaInitializer schemaInitializer;

    @Autowired
    private JdbcTemplate jdbcTemplate;


    @GetMapping
    public List<String> getAllSchemas() {
        return jdbcTemplate.queryForList("SELECT schema_name FROM information_schema.schemata", String.class)
                .stream()
                .filter(name -> !name.startsWith("pg_") && !name.equals("information_schema"))
                .collect(Collectors.toList());
    }

    @PostMapping("/create-and-init")
    public ResponseEntity<String> createAndInit(@RequestParam String name) {
        if (schemaService.schemaExists(name)) {
            return ResponseEntity.badRequest().body("Schema already exists");
        }

        schemaService.createSchema(name);
        schemaInitializer.createTablesForSchema(name);
        return ResponseEntity.ok("Schema and tables created for: " + name);
    }

    @GetMapping("/current")
    public ResponseEntity<String> getCurrentTenant() {
        String currentSchema = TenantContext.getTenant();
        if (currentSchema == null) {
            return ResponseEntity.ok("No schema (tenant) set for this request.");
        }
        return ResponseEntity.ok("Current connected schema: " + currentSchema);
    }






}
