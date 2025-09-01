package com.thinkerscave.common.admission.controller;

import com.thinkerscave.common.admission.dto.ApplicationAdmissionCreateRequest;
import com.thinkerscave.common.admission.dto.ApplicationAdmissionDraftRequest;
import com.thinkerscave.common.admission.dto.ApplicationAdmissionEditRequest;
import com.thinkerscave.common.admission.dto.ApplicationAdmissionResponse;
import com.thinkerscave.common.admission.service.ApplicationAdmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

/**
 * REST controller for ApplicationAdmission APIs.
 * Author: Bibek
 */
@RestController
@RequestMapping("/api/admissions")
@RequiredArgsConstructor
@Tag(name = "Application Admission", description = "APIs for managing application admissions")
@CrossOrigin("*")
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
    public ResponseEntity<ApplicationAdmissionResponse> edit(@PathVariable Long id, @RequestBody ApplicationAdmissionEditRequest request) {
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
    public ResponseEntity<ApplicationAdmissionResponse> getById(@PathVariable Long id) {
        Optional<ApplicationAdmissionResponse> response = service.getById(id);
        return response.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }
}
