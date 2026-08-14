package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.AcademicYearRequest;
import com.thinkerscave.academics.dto.request.RejectAcademicYearRequest;
import com.thinkerscave.academics.dto.response.AcademicYearDashboardResponse;
import com.thinkerscave.academics.dto.response.AcademicYearResponse;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.service.AcademicYearService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academics/years")
@RequiredArgsConstructor
@Tag(name = "Academic Year", description = "Academic year lifecycle and dashboard")
public class AcademicYearController {

    private final AcademicYearService academicYearService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Academic Year page dashboard (current, upcoming, history)")
    public ResponseEntity<ApiResponse<AcademicYearDashboardResponse>> dashboard() {
        return ResponseEntity.ok(ApiResponse.success("Academic year dashboard", academicYearService.getDashboard()));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Search academic years")
    public ResponseEntity<ApiResponse<Page<AcademicYearResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) AcademicYearStatus status,
            @PageableDefault(size = 20, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Academic years retrieved",
                academicYearService.search(q, status, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Get academic year by ID")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Academic year found", academicYearService.getById(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Create academic year")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> create(@Valid @RequestBody AcademicYearRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Academic year created", academicYearService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Update academic year")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> update(
            @PathVariable Long id, @Valid @RequestBody AcademicYearRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Academic year updated", academicYearService.update(id, request)));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Deactivate academic year (is_active = false)")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> deactivate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Academic year deactivated", academicYearService.deactivate(id)));
    }

    @PostMapping("/{id}/ready")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Mark academic year ready for approval")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> markReady(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Marked ready for approval", academicYearService.markReadyForApproval(id)));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Submit academic year for approval")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> submit(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Submitted for approval", academicYearService.submitForApproval(id)));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Approve academic year")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> approve(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Academic year approved", academicYearService.approve(id)));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Reject academic year")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> reject(
            @PathVariable Long id, @Valid @RequestBody RejectAcademicYearRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Academic year rejected", academicYearService.reject(id, request)));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Activate academic year as CURRENT")
    public ResponseEntity<ApiResponse<AcademicYearResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Academic year activated", academicYearService.activate(id)));
    }
}
