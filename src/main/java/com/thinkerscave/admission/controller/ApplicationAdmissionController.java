package com.thinkerscave.admission.controller;

import com.thinkerscave.admission.dto.request.ApplicationAdmissionRequest;
import com.thinkerscave.admission.dto.response.ApplicationAdmissionResponse;
import com.thinkerscave.admission.enums.ApplicationStatus;
import com.thinkerscave.admission.service.ApplicationAdmissionService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admission/applications")
@RequiredArgsConstructor
@Tag(name = "Admission - Applications", description = "Manage student admission applications")
@PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'TEACHER')")
public class ApplicationAdmissionController {

    private final ApplicationAdmissionService service;

    @PostMapping("/draft")
    @Operation(summary = "Save application as draft")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> saveDraft(
            @Valid @RequestBody ApplicationAdmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Draft saved", service.saveDraft(request)));
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit a new application")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> submit(
            @Valid @RequestBody ApplicationAdmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Application submitted", service.submit(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a draft application")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> update(
            @PathVariable Long id, @Valid @RequestBody ApplicationAdmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Application updated", service.update(id, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application by ID")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Application fetched", service.getById(id)));
    }

    @GetMapping
    @Operation(summary = "Get all applications (paged)")
    public ResponseEntity<ApiResponse<Page<ApplicationAdmissionResponse>>> getAll(
            @PageableDefault(size = 20, sort = "createdOn") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Applications fetched", service.getAll(pageable)));
    }

    @GetMapping("/by-status")
    @Operation(summary = "Get applications by status (paged)")
    public ResponseEntity<ApiResponse<Page<ApplicationAdmissionResponse>>> getByStatus(
            @RequestParam ApplicationStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Applications by status", service.getByStatus(status, pageable)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update application status")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status,
            @RequestParam(required = false) String comments) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", service.updateStatus(id, status, comments)));
    }
}
