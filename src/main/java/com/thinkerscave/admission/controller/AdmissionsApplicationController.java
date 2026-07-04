package com.thinkerscave.admission.controller;

import com.thinkerscave.admission.dto.request.ApplicationAdmissionRequest;
import com.thinkerscave.admission.dto.request.ApplicationSearchRequest;
import com.thinkerscave.admission.dto.response.ApplicationAdmissionResponse;
import com.thinkerscave.admission.dto.response.ApplicationProgressResponse;
import com.thinkerscave.admission.enums.ApplicationStatus;
import com.thinkerscave.admission.service.ApplicationAdmissionService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admissions/applications")
@RequiredArgsConstructor
@Tag(name = "Admissions CRM - Applications")
public class AdmissionsApplicationController {

    private final ApplicationAdmissionService applicationService;

    @PostMapping("/draft")
    @Operation(summary = "Save application draft")
    @PreAuthorize("hasAnyAuthority('ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','PRINCIPAL','HR_MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> saveDraft(@Valid @RequestBody ApplicationAdmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Draft saved", applicationService.saveDraft(request)));
    }

    @PostMapping("/submit")
    @Operation(summary = "Submit application")
    @PreAuthorize("hasAnyAuthority('ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','PRINCIPAL','HR_MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> submit(@Valid @RequestBody ApplicationAdmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Application submitted", applicationService.submit(request)));
    }

    @GetMapping
    @Operation(summary = "List applications")
    @PreAuthorize("hasAnyAuthority('ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','PRINCIPAL','HR_MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<Page<ApplicationAdmissionResponse>>> list(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Applications loaded", applicationService.getAll(pageable)));
    }

    @PostMapping("/search")
    @Operation(summary = "Search applications")
    @PreAuthorize("hasAnyAuthority('ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','PRINCIPAL','HR_MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<Page<ApplicationAdmissionResponse>>> search(
            @RequestBody(required = false) ApplicationSearchRequest request,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Application search completed", applicationService.search(request, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get application detail")
    @PreAuthorize("hasAnyAuthority('ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','PRINCIPAL','HR_MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Application loaded", applicationService.getById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update application")
    @PreAuthorize("hasAnyAuthority('ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','PRINCIPAL','HR_MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ApplicationAdmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Application updated", applicationService.update(id, request)));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve application")
    @PreAuthorize("hasAnyAuthority('ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','PRINCIPAL','HR_MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> approve(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ApiResponse.success("Application approved", applicationService.approve(id, remarks)));
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject application")
    @PreAuthorize("hasAnyAuthority('ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','PRINCIPAL','HR_MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> reject(
            @PathVariable Long id,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ApiResponse.success("Application rejected", applicationService.reject(id, remarks)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update application status")
    @PreAuthorize("hasAnyAuthority('ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','PRINCIPAL','HR_MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam ApplicationStatus status,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ApiResponse.success("Application status updated", applicationService.updateStatus(id, status, remarks)));
    }

    @GetMapping("/{id}/progress")
    @Operation(summary = "Get wizard progress")
    @PreAuthorize("hasAnyAuthority('ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','PRINCIPAL','HR_MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<ApplicationProgressResponse>> progress(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Application progress loaded", applicationService.getProgress(id)));
    }
}