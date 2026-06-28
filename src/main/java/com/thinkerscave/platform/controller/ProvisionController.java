package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.request.ProvisionOrganizationRequest;
import com.thinkerscave.platform.dto.response.ProvisioningJobResponse;
import com.thinkerscave.platform.dto.response.ProvisioningResultResponse;
import com.thinkerscave.platform.enums.ProvisionJobStatus;
import com.thinkerscave.platform.service.ProvisionService;
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
@RequestMapping("/api/platform/provision")
@RequiredArgsConstructor
@Tag(name = "Organization Provisioning", description = "Provision new organization workspaces end-to-end")
public class ProvisionController {

    private final ProvisionService provisionService;

    @PostMapping
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Provision a complete organization workspace (18-step workflow)")
    public ResponseEntity<ApiResponse<ProvisioningResultResponse>> provision(
            @Valid @RequestBody ProvisionOrganizationRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Organization provisioned successfully", provisionService.provision(request)));
    }

    @GetMapping("/jobs")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "List provisioning jobs")
    public ResponseEntity<ApiResponse<Page<ProvisioningJobResponse>>> getJobs(
            @RequestParam(required = false) ProvisionJobStatus status,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Provisioning jobs retrieved",
                provisionService.getJobs(status, search, pageable)));
    }

    @GetMapping("/jobs/{id}")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Get provisioning job detail")
    public ResponseEntity<ApiResponse<ProvisioningJobResponse>> getJobById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Provisioning job retrieved", provisionService.getJobById(id)));
    }

    @GetMapping("/jobs/{id}/logs")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Get provisioning job logs")
    public ResponseEntity<ApiResponse<ProvisioningJobResponse>> getJobLogs(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Provisioning job logs retrieved", provisionService.getJobLogs(id)));
    }

    @PostMapping("/jobs/{id}/retry")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Retry a failed provisioning job")
    public ResponseEntity<ApiResponse<ProvisioningJobResponse>> retryJob(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Job queued for retry", provisionService.retryJob(id)));
    }
}
