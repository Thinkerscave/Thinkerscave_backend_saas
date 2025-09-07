package com.thinkerscave.common.admission.controller;

import com.thinkerscave.common.admission.domain.ApplicationStatus;
import com.thinkerscave.common.admission.dto.*;
import com.thinkerscave.common.admission.service.ApplicationAdmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

/**
 * REST controller for ApplicationAdmission APIs.
 * Author: Bibek
 */
@RestController
@RequestMapping("/api/admissions")
@RequiredArgsConstructor
@Tag(name = "Application Admission", description = "APIs for managing application admissions")
@CrossOrigin(origins = "http://localhost:4200")
public class ApplicationAdmissionController {

    private final ApplicationAdmissionService service;
    /**
     * Saves an application form as a draft.
     * @param request DTO with partial or complete application data.
     * @return The created or updated draft.
     */
    @Operation(summary = "Save Application as Draft", description = "Saves the current application progress as a draft. Can be used to create a new draft or update an existing one.")
    @PostMapping("/draft")
    public ResponseEntity<ApplicationAdmissionResponse> saveDraft(@RequestBody ApplicationAdmissionDraftRequest request) {
        ApplicationAdmissionResponse draftResponse = service.saveDraft(request);
        return new ResponseEntity<>(draftResponse, HttpStatus.CREATED);
    }
    /**
     * Create a new ApplicationAdmission.
     * @param request DTO with application data
     * @return Created ApplicationAdmissionResponse
     */
    @Operation(summary = "Create Application Admission", description = "Creates a new application admission with applicant details.")
    @PostMapping
    public ResponseEntity<ApplicationAdmissionResponse> create(@RequestBody ApplicationAdmissionCreateRequest request) {
        return ResponseEntity.ok(service.create(request));
    }

    /**
     * Edit an existing ApplicationAdmission by id.
     * @param id ApplicationAdmission id
     * @param request DTO with fields to update
     * @return Updated ApplicationAdmissionResponse if found
     */
    @Operation(summary = "Edit Application Admission", description = "Edits an existing application admission by its ID.")
    @PutMapping("/{id}")
    public ResponseEntity<ApplicationAdmissionResponse> edit(@PathVariable String id, @RequestBody ApplicationAdmissionEditRequest request) {
        Optional<ApplicationAdmissionResponse> response = service.edit(id, request);
        return response.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Get ApplicationAdmission by id.
     * @param id ApplicationAdmission id
     * @return ApplicationAdmissionResponse if found
     */
    @Operation(summary = "Get Application Admission by ID", description = "Retrieves an application admission by its ID.")
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationAdmissionResponse> getById(@PathVariable String id) {
        Optional<ApplicationAdmissionResponse> response = service.getById(id);
        return response.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * Update the status of multiple applications (APPROVED/REJECTED).
     * @param request Contains the status and list of application IDs to update
     * @return Response with update statistics and any errors
     */
    @Operation(summary = "Update Application Status", 
              description = "Updates the status of multiple applications to either APPROVED or REJECTED. " +
                          "Approved applications will create corresponding student records.")
    @PostMapping("/status")
    public ResponseEntity<ApplicationStatusUpdateResponse> updateApplicationStatus(
            @Valid @RequestBody ApplicationStatusUpdateRequest request) {
        // Validate status is either APPROVED or REJECTED
        if (request.getStatus() != ApplicationStatus.APPROVED &&
            request.getStatus() != ApplicationStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Status must be either APPROVED or REJECTED");
        }
        
        ApplicationStatusUpdateResponse response = service.updateApplicationStatus(request);
        return ResponseEntity.ok(response);
    }
}
