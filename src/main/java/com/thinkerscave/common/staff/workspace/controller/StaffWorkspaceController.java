package com.thinkerscave.common.staff.workspace.controller;

import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.staff.workspace.dto.StaffWorkspaceDtos.*;
import com.thinkerscave.common.staff.workspace.service.StaffWorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff/workspace")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PRINCIPAL', 'HR_MANAGER', 'TEACHER', 'STAFF')")
public class StaffWorkspaceController {

    private final StaffWorkspaceService service;

    // KPI
    @GetMapping("/kpi")
    public ResponseEntity<ApiResponse<StaffKpi>> kpi() {
        return ResponseEntity.ok(ApiResponse.success(service.kpi()));
    }

    // Directory
    @PostMapping("/directory/search")
    public ResponseEntity<ApiResponse<List<StaffDirectoryCard>>> directorySearch(@RequestBody(required = false) StaffSearchRequest req) {
        return ResponseEntity.ok(ApiResponse.success(service.directorySearch(req)));
    }

    // Profile 360
    @GetMapping("/employees/{staffId}/profile-360")
    public ResponseEntity<ApiResponse<StaffProfile360>> profile360(@PathVariable Long staffId) {
        return ResponseEntity.ok(ApiResponse.success(service.profile360(staffId)));
    }

    @GetMapping("/employees/{staffId}/timeline")
    public ResponseEntity<ApiResponse<List<StaffTimelineEntry>>> timeline(@PathVariable Long staffId) {
        return ResponseEntity.ok(ApiResponse.success(service.timeline(staffId)));
    }

    // Teaching Profile
    @PostMapping("/employees/teaching-profile")
    public ResponseEntity<ApiResponse<StaffTeachingSnapshot>> saveTeachingProfile(@Valid @RequestBody TeachingProfileRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Teaching profile saved", service.saveTeachingProfile(req)));
    }

    // Responsibilities
    @GetMapping("/responsibilities/kpi")
    public ResponseEntity<ApiResponse<ResponsibilityKpi>> responsibilityKpi() {
        return ResponseEntity.ok(ApiResponse.success(service.responsibilityKpi()));
    }

    @GetMapping("/responsibilities")
    public ResponseEntity<ApiResponse<List<ResponsibilityResponse>>> listResponsibilities() {
        return ResponseEntity.ok(ApiResponse.success(service.responsibilities()));
    }

    @PostMapping("/responsibilities")
    public ResponseEntity<ApiResponse<ResponsibilityResponse>> addResponsibility(@Valid @RequestBody ResponsibilityRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Responsibility created", service.addResponsibility(req)));
    }

    @PutMapping("/responsibilities/{id}")
    public ResponseEntity<ApiResponse<ResponsibilityResponse>> updateResponsibility(@PathVariable Long id,
                                                                                     @Valid @RequestBody ResponsibilityRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Responsibility updated", service.updateResponsibility(id, req)));
    }

    @DeleteMapping("/responsibilities/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteResponsibility(@PathVariable Long id) {
        service.deleteResponsibility(id);
        return ResponseEntity.ok(ApiResponse.success("Responsibility deleted", null));
    }

    // Leave & Availability
    @GetMapping("/leave/kpi")
    public ResponseEntity<ApiResponse<LeaveAvailabilityKpi>> leaveKpi() {
        return ResponseEntity.ok(ApiResponse.success(service.leaveAvailabilityKpi()));
    }

    @GetMapping("/leave/today")
    public ResponseEntity<ApiResponse<List<TodayLeaveEntry>>> todayLeaves() {
        return ResponseEntity.ok(ApiResponse.success(service.todayLeaves()));
    }

    // Documents Vault
    @GetMapping("/documents/kpi")
    public ResponseEntity<ApiResponse<StaffDocumentKpi>> documentKpi() {
        return ResponseEntity.ok(ApiResponse.success(service.documentKpi()));
    }

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<StaffDocumentEntry>>> documents(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long staffId) {
        return ResponseEntity.ok(ApiResponse.success(service.documents(category, staffId)));
    }

    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<StaffDocumentEntry>> addDocument(@Valid @RequestBody StaffDocumentRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Document uploaded", service.addDocument(req)));
    }

    @PostMapping("/documents/{id}/verify")
    public ResponseEntity<ApiResponse<StaffDocumentEntry>> verifyDocument(@PathVariable Long id, Authentication auth) {
        String verifier = auth != null ? auth.getName() : "SYSTEM";
        return ResponseEntity.ok(ApiResponse.success("Document verified", service.verifyDocument(id, verifier)));
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
        service.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success("Document deleted", null));
    }

    // Alumni Staff
    @GetMapping("/alumni/kpi")
    public ResponseEntity<ApiResponse<AlumniStaffKpi>> alumniKpi() {
        return ResponseEntity.ok(ApiResponse.success(service.alumniKpi()));
    }

    @GetMapping("/alumni")
    public ResponseEntity<ApiResponse<List<AlumniStaffResponse>>> alumni() {
        return ResponseEntity.ok(ApiResponse.success(service.alumniList()));
    }

    @PostMapping("/alumni")
    public ResponseEntity<ApiResponse<AlumniStaffResponse>> addAlumni(@Valid @RequestBody AlumniStaffRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Alumni staff record created", service.addAlumni(req)));
    }
}
