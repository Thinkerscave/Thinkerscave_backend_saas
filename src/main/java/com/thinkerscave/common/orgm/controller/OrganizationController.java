package com.thinkerscave.common.orgm.controller;

import com.thinkerscave.common.exception.SchemaCreationException;
import com.thinkerscave.common.orgm.config.TenantContext;
import com.thinkerscave.common.orgm.requestDto.OrgRegistrationRequest;
import com.thinkerscave.common.orgm.responseDto.OrgRegistrationResponse;
import com.thinkerscave.common.orgm.service.OrganizationService;
import com.thinkerscave.common.orgm.service.SchemaInitializer;
import com.thinkerscave.common.orgm.service.SchemaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SchemaInitializer schemaInitializer;

    @PostMapping("/register")
    public ResponseEntity<?> registerOrganization(@RequestBody OrgRegistrationRequest request) {
        String schema = TenantContext.getTenant();  // This will be tenant/schema name

        try {
            if (schemaService.schemaExists(schema)) {
                return ResponseEntity.badRequest().body("Schema already exists");
            }

            // Create Schema and Tables
            schemaService.createSchema(schema);
            schemaInitializer.createTablesForSchema(schema);

            // Register Organization
            OrgRegistrationResponse response = organizationService.registerOrg(request);
            return ResponseEntity.ok(response);

        } catch (SchemaCreationException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create schema: " + e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }
}
