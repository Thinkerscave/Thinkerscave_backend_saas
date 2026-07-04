package com.thinkerscave.admission.controller;

import com.thinkerscave.admission.dto.request.ApplicationAdmissionRequest;
import com.thinkerscave.admission.dto.request.FollowUpRequest;
import com.thinkerscave.admission.dto.response.ApplicationAdmissionResponse;
import com.thinkerscave.admission.dto.response.FollowUpResponse;
import com.thinkerscave.admission.service.ApplicationAdmissionService;
import com.thinkerscave.admission.service.InquiryService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Admissions CRM - Compatibility")
public class AdmissionsCompatibilityController {

    private final InquiryService inquiryService;
    private final ApplicationAdmissionService applicationService;

    @PostMapping("/api/v1/inquiries/{id}/proceed-admission")
    @Operation(summary = "Compatibility endpoint: proceed lead to application")
    @PreAuthorize("hasAnyAuthority('ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','PRINCIPAL','HR_MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> proceedAdmission(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Application initialized", inquiryService.convertToApplication(id)));
    }

    @PostMapping("/api/v1/inquiries/{id}/follow-ups")
    @Operation(summary = "Compatibility endpoint: add follow-up")
    @PreAuthorize("hasAnyAuthority('ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','PRINCIPAL','HR_MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<FollowUpResponse>> addFollowUp(@PathVariable Long id,
                                                                      @Valid @RequestBody FollowUpRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Follow-up added", inquiryService.addFollowUp(id, request)));
    }

    @PostMapping("/api/v1/admissions/draft")
    @Operation(summary = "Compatibility endpoint: save application draft")
    @PreAuthorize("hasAnyAuthority('ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','PRINCIPAL','HR_MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> saveDraft(@Valid @RequestBody ApplicationAdmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Draft saved", applicationService.saveDraft(request)));
    }

    @PostMapping("/api/v1/admissions")
    @Operation(summary = "Compatibility endpoint: submit application")
    @PreAuthorize("hasAnyAuthority('ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','PRINCIPAL','HR_MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> submit(@Valid @RequestBody ApplicationAdmissionRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Application submitted", applicationService.submit(request)));
    }

    @GetMapping("/api/v1/admissions/{id}")
    @Operation(summary = "Compatibility endpoint: get application")
    @PreAuthorize("hasAnyAuthority('ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','PRINCIPAL','HR_MANAGER','STAFF')")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> getAdmission(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Application loaded", applicationService.getById(id)));
    }
}