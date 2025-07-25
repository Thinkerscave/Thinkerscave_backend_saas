package com.thinkerscave.common.orgm.controller;

import com.thinkerscave.common.exception.SchemaCreationException;
import com.thinkerscave.common.config.TenantContext;
import com.thinkerscave.common.orgm.domain.Organisation;

import com.thinkerscave.common.orgm.dto.OrgRequestDTO;
import com.thinkerscave.common.orgm.dto.OrgResponseDTO;
import com.thinkerscave.common.orgm.dto.OwnerDTO;
import com.thinkerscave.common.orgm.service.OrganizationService;
import com.thinkerscave.common.orgm.service.SchemaInitializer;
import com.thinkerscave.common.orgm.service.SchemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

@CrossOrigin("*")
@RestController
@RequestMapping("/api/organizations")
@Tag(name = "Organization Management", description = "APIs related to organization registration and management")
public class OrganizationController {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private SchemaService schemaService;

    @Autowired
    private SchemaInitializer schemaInitializer;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new organization",
            description = "Registers a new organization and creates its corresponding schema.",
            parameters = {
                    @Parameter(name = "X-Tenant-ID", description = "Schema Name", required = true,
                            example = "demo_org", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Successfully registered organization",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = OrgResponseDTO.class),
                                    examples = @ExampleObject(value = """
                        {
                          "message": "Organization registered successfully",
                          "orgCode": "demo_org",
                          "userCode": "admin_user"
                        }
                    """)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Schema already exists"),
                    @ApiResponse(responseCode = "500", description = "Internal server error")
            }
    )
    public ResponseEntity<?> registerOrganization(@RequestBody OrgRequestDTO request) {
        String schema = TenantContext.getTenant();  // This will be tenant/schema name

        try {
//            if (schemaService.schemaExists(schema)) {
//                return ResponseEntity.badRequest().body("Schema already exists");
//            }
//
//            // Create Schema and Tables
//            schemaService.createSchema(schema);
//            schemaInitializer.createTablesForSchema(schema);

            // Register Organization
            OrgResponseDTO response = organizationService.saveOrganization(request);
            return ResponseEntity.ok(response);

        } catch (SchemaCreationException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to create schema: " + e.getMessage());

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }

    @GetMapping("/all")
    @Operation(
            summary = "Get all organizations",
            parameters = {
                    @Parameter(name = "X-Tenant-ID", description = "Schema Name", required = true,
                            example = "master", in = io.swagger.v3.oas.annotations.enums.ParameterIn.HEADER)
            }
    )
    public ResponseEntity<List<Organisation>> getAllOrganizations() {
        try {
            List<Organisation> organizations = organizationService.getAllOrgs();
            return ResponseEntity.ok(organizations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Collections.emptyList());
        }
    }

    @DeleteMapping("/{orgCode}")
    public ResponseEntity<String> softDeleteOrg(@PathVariable String orgCode) {
        String result = organizationService.softDeleteOrg(orgCode);
        return ResponseEntity.ok(result);
    }


    @PutMapping("/owner/update")
    public ResponseEntity<String> updateOwnerDetails(@RequestBody OwnerDTO dto) {
        organizationService.updateOwnerDetailsWithUser(dto);
        return ResponseEntity.ok("Owner and user details updated successfully.");
    }






}
