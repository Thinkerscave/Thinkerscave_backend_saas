package com.thinkerscave.admission.controller;

import com.thinkerscave.admission.dto.request.ApplicationSearchRequest;
import com.thinkerscave.admission.dto.request.AssignCounselorRequest;
import com.thinkerscave.admission.dto.request.CounselingNoteRequest;
import com.thinkerscave.admission.dto.request.LeadSearchRequest;
import com.thinkerscave.admission.dto.response.AdmissionKpiResponse;
import com.thinkerscave.admission.dto.response.AdmissionsSettingsResponse;
import com.thinkerscave.admission.dto.response.ApplicationAdmissionResponse;
import com.thinkerscave.admission.dto.response.ApplicationProgressResponse;
import com.thinkerscave.admission.dto.response.CounselingNoteResponse;
import com.thinkerscave.admission.dto.response.FollowUpResponse;
import com.thinkerscave.admission.dto.response.InquiryFullDetailResponse;
import com.thinkerscave.admission.dto.response.InquiryQuickActionResponse;
import com.thinkerscave.admission.dto.response.InquiryResponse;
import com.thinkerscave.admission.dto.response.InquiryTimelineItemResponse;
import com.thinkerscave.admission.dto.response.InquiryWorkspaceKpiResponse;
import com.thinkerscave.admission.enums.InquiryStatus;
import com.thinkerscave.admission.service.ApplicationAdmissionService;
import com.thinkerscave.admission.service.InquiryService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admissions/workspace")
@RequiredArgsConstructor
@Tag(name = "Admissions CRM - Workspace")
public class AdmissionsWorkspaceController {

    private final InquiryService inquiryService;
    private final ApplicationAdmissionService applicationService;

    @GetMapping("/inquiries/kpi")
    @Operation(summary = "Workspace inquiry KPI")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<InquiryWorkspaceKpiResponse>> inquiryKpi() {
        return ResponseEntity.ok(ApiResponse.success("Inquiry KPI loaded", inquiryService.getWorkspaceKpi()));
    }

    @GetMapping("/inquiries/quick-actions")
    @Operation(summary = "Workspace quick action counters")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<InquiryQuickActionResponse>> inquiryQuickActions() {
        return ResponseEntity.ok(ApiResponse.success("Quick actions loaded", inquiryService.getQuickActions()));
    }

    @PostMapping("/inquiries/search")
    @Operation(summary = "Workspace inquiry search")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<Page<InquiryResponse>>> inquirySearch(
            @RequestBody(required = false) LeadSearchRequest request,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Inquiries loaded", inquiryService.search(request, pageable)));
    }

    @GetMapping("/inquiries/{id}/full")
    @Operation(summary = "Workspace inquiry full detail")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<InquiryFullDetailResponse>> fullDetail(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Inquiry detail loaded", inquiryService.getFullDetail(id)));
    }

    @GetMapping("/inquiries/{id}/timeline")
    @Operation(summary = "Workspace inquiry timeline")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<List<InquiryTimelineItemResponse>>> timeline(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Timeline loaded", inquiryService.getTimeline(id)));
    }

    @PutMapping("/inquiries/{id}/assign-counselor")
    @Operation(summary = "Workspace assign counselor")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<InquiryResponse>> assignCounselor(
            @PathVariable Long id,
            @Valid @RequestBody AssignCounselorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Counselor assigned",
                inquiryService.assignCounselor(id, request.getCounselorId())));
    }

    @PostMapping("/inquiries/{id}/mark-interested")
    @Operation(summary = "Workspace mark interested")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<InquiryResponse>> markInterested(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Inquiry marked interested",
                inquiryService.updateStatus(id, InquiryStatus.INTERESTED)));
    }

    @PostMapping("/inquiries/{id}/mark-closed")
    @Operation(summary = "Workspace mark closed")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<InquiryResponse>> markClosed(@PathVariable Long id,
                                                                    @RequestParam(required = false) String reason) {
        if (reason != null && !reason.isBlank()) {
            return ResponseEntity.ok(ApiResponse.success("Inquiry marked closed", inquiryService.markLost(id, reason)));
        }
        return ResponseEntity.ok(ApiResponse.success("Inquiry marked closed", inquiryService.updateStatus(id, InquiryStatus.CLOSED)));
    }

    @GetMapping("/inquiries/{id}/counseling-notes")
    @Operation(summary = "Workspace counseling notes")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<List<CounselingNoteResponse>>> counselingNotes(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Counseling notes loaded", inquiryService.getCounselingNotes(id)));
    }

    @PostMapping("/inquiries/{id}/counseling-notes")
    @Operation(summary = "Workspace add counseling note")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<CounselingNoteResponse>> addCounselingNote(@PathVariable Long id,
                                                                                  @Valid @RequestBody CounselingNoteRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Counseling note added", inquiryService.addCounselingNote(id, request)));
    }

    @GetMapping("/admissions/kpi")
    @Operation(summary = "Workspace admissions KPI")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<AdmissionKpiResponse>> admissionKpi() {
        return ResponseEntity.ok(ApiResponse.success("Admissions KPI loaded", inquiryService.getKpi()));
    }

    @PostMapping("/admissions/search")
    @Operation(summary = "Workspace admissions search")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<Page<ApplicationAdmissionResponse>>> admissionSearch(
            @RequestBody(required = false) ApplicationSearchRequest request,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Applications loaded", applicationService.search(request, pageable)));
    }

    @GetMapping("/admissions/{id}/progress")
    @Operation(summary = "Workspace wizard progress")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<ApplicationProgressResponse>> progress(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Progress loaded", applicationService.getProgress(id)));
    }

    @GetMapping("/settings")
    @Operation(summary = "Workspace settings")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<AdmissionsSettingsResponse>> settings() {
        AdmissionsSettingsResponse response = AdmissionsSettingsResponse.builder()
                .inquirySources(List.of("Website", "Walk-in", "Referral", "Social Media", "Campaign"))
                .inquiryStatuses(List.of("NEW", "CONTACTED", "INTERESTED", "COUNSELING", "READY_FOR_ADMISSION", "LOST", "CLOSED"))
                .requiredDocuments(List.of("BIRTH_CERTIFICATE", "AADHAR", "TRANSFER_CERTIFICATE", "PHOTO", "MARKSHEET"))
                .numbering(Map.of("leadPrefix", "LD", "applicationPrefix", "APP", "admissionPrefix", "ADM"))
                .reminderRules(Map.of("defaultMode", "AUTO", "defaultLeadTime", "24H"))
                .build();
        return ResponseEntity.ok(ApiResponse.success("Settings loaded", response));
    }
}