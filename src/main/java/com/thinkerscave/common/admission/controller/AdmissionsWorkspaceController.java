package com.thinkerscave.common.admission.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thinkerscave.common.admission.dto.AdmissionKpiResponse;
import com.thinkerscave.common.admission.dto.AdmissionProgressResponse;
import com.thinkerscave.common.admission.dto.AdmissionSearchRequest;
import com.thinkerscave.common.admission.dto.AdmissionsSettingsResponse;
import com.thinkerscave.common.admission.dto.ApplicationAdmissionResponse;
import com.thinkerscave.common.admission.dto.AssignCounselorRequest;
import com.thinkerscave.common.admission.dto.CounselingNoteRequest;
import com.thinkerscave.common.admission.dto.CounselingNoteResponse;
import com.thinkerscave.common.admission.dto.InquiryFullDetailResponse;
import com.thinkerscave.common.admission.dto.InquiryKpiResponse;
import com.thinkerscave.common.admission.dto.InquiryQuickActionsResponse;
import com.thinkerscave.common.admission.dto.InquiryResponse;
import com.thinkerscave.common.admission.dto.InquirySearchRequest;
import com.thinkerscave.common.admission.dto.InquiryTimelineEntry;
import com.thinkerscave.common.admission.service.AdmissionsWorkspaceService;
import com.thinkerscave.common.dto.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Endpoints powering the spec-aligned Admissions module workspace
 * (Inquiry Center, Inquiry 360 detail, Admission Center, Settings).
 */
@RestController
@RequestMapping("/api/v1/admissions/workspace")
@Tag(name = "Admissions Workspace", description = "Spec-aligned admissions workspace APIs")
@RequiredArgsConstructor
public class AdmissionsWorkspaceController {

    private final AdmissionsWorkspaceService workspaceService;

    // ============ Inquiry Center ============

    @Operation(summary = "Inquiry Center KPI summary")
    @GetMapping("/inquiries/kpi")
    public ResponseEntity<ApiResponse<InquiryKpiResponse>> inquiryKpi() {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.inquiryKpiSummary()));
    }

    @Operation(summary = "Inquiry Center quick actions")
    @GetMapping("/inquiries/quick-actions")
    public ResponseEntity<ApiResponse<InquiryQuickActionsResponse>> inquiryQuickActions() {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.inquiryQuickActions()));
    }

    @Operation(summary = "Search inquiries with filters")
    @PostMapping("/inquiries/search")
    public ResponseEntity<ApiResponse<List<InquiryResponse>>> searchInquiries(
            @RequestBody InquirySearchRequest filter) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.searchInquiries(filter)));
    }

    // ============ Inquiry 360 Detail ============

    @Operation(summary = "Full inquiry detail bundle for the 360 workspace")
    @GetMapping("/inquiries/{inquiryId}/full")
    public ResponseEntity<ApiResponse<InquiryFullDetailResponse>> fullDetail(@PathVariable Long inquiryId) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.fullDetail(inquiryId)));
    }

    @Operation(summary = "Inquiry timeline (chronological immutable)")
    @GetMapping("/inquiries/{inquiryId}/timeline")
    public ResponseEntity<ApiResponse<List<InquiryTimelineEntry>>> timeline(@PathVariable Long inquiryId) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.inquiryTimeline(inquiryId)));
    }

    // ============ Inquiry Actions ============

    @Operation(summary = "Assign counselor to an inquiry")
    @PutMapping("/inquiries/{inquiryId}/assign-counselor")
    public ResponseEntity<ApiResponse<InquiryResponse>> assignCounselor(
            @PathVariable Long inquiryId,
            @Valid @RequestBody AssignCounselorRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success(workspaceService.assignCounselor(inquiryId, request.getCounselorId())));
    }

    @Operation(summary = "Mark inquiry as interested")
    @PostMapping("/inquiries/{inquiryId}/mark-interested")
    public ResponseEntity<ApiResponse<InquiryResponse>> markInterested(@PathVariable Long inquiryId) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.markInterested(inquiryId)));
    }

    @Operation(summary = "Mark inquiry as closed")
    @PostMapping("/inquiries/{inquiryId}/mark-closed")
    public ResponseEntity<ApiResponse<InquiryResponse>> markClosed(
            @PathVariable Long inquiryId,
            @RequestParam(required = false) String reason) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.markClosed(inquiryId, reason)));
    }

    // ============ Counseling Notes ============

    @Operation(summary = "List counseling notes for an inquiry")
    @GetMapping("/inquiries/{inquiryId}/counseling-notes")
    public ResponseEntity<ApiResponse<List<CounselingNoteResponse>>> counselingNotes(@PathVariable Long inquiryId) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.counselingNotes(inquiryId)));
    }

    @Operation(summary = "Add counseling note to an inquiry")
    @PostMapping("/inquiries/{inquiryId}/counseling-notes")
    public ResponseEntity<ApiResponse<CounselingNoteResponse>> addCounselingNote(
            @PathVariable Long inquiryId,
            @RequestBody CounselingNoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.addCounselingNote(inquiryId, request)));
    }

    // ============ Admission Center ============

    @Operation(summary = "Admission Center KPI summary")
    @GetMapping("/admissions/kpi")
    public ResponseEntity<ApiResponse<AdmissionKpiResponse>> admissionKpi() {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.admissionKpiSummary()));
    }

    @Operation(summary = "Search admissions with filters")
    @PostMapping("/admissions/search")
    public ResponseEntity<ApiResponse<List<ApplicationAdmissionResponse>>> searchAdmissions(
            @RequestBody AdmissionSearchRequest filter) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.searchAdmissions(filter)));
    }

    @Operation(summary = "7-step admission wizard progress")
    @GetMapping("/admissions/{applicationId}/progress")
    public ResponseEntity<ApiResponse<AdmissionProgressResponse>> progress(@PathVariable String applicationId) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.admissionProgress(applicationId)));
    }

    // ============ Settings ============

    @Operation(summary = "Admissions settings (single page, card-based)")
    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<AdmissionsSettingsResponse>> settings() {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.settings()));
    }
}
