package com.thinkerscave.admission.controller;

import com.thinkerscave.admission.dto.request.CounselingNoteRequest;
import com.thinkerscave.admission.dto.request.FollowUpRequest;
import com.thinkerscave.admission.dto.request.InquiryRequest;
import com.thinkerscave.admission.dto.response.AdmissionKpiResponse;
import com.thinkerscave.admission.dto.response.CounselingNoteResponse;
import com.thinkerscave.admission.dto.response.FollowUpResponse;
import com.thinkerscave.admission.dto.response.InquiryResponse;
import com.thinkerscave.admission.enums.InquiryStatus;
import com.thinkerscave.admission.service.InquiryService;
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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admission/inquiries")
@RequiredArgsConstructor
@Tag(name = "Admission - Inquiries", description = "Manage admission prospect inquiries")
@PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN', 'ORGANIZATION_OWNER', 'STAFF')")
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    @Operation(summary = "Create a new inquiry")
    public ResponseEntity<ApiResponse<InquiryResponse>> create(@Valid @RequestBody InquiryRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Inquiry created", inquiryService.create(request)));
    }

    @GetMapping
    @Operation(summary = "Get all inquiries (paged)")
    public ResponseEntity<ApiResponse<Page<InquiryResponse>>> getAll(
            @PageableDefault(size = 20, sort = "createdOn") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Inquiries fetched", inquiryService.getAll(pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get inquiry by ID")
    public ResponseEntity<ApiResponse<InquiryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Inquiry fetched", inquiryService.getById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update inquiry")
    public ResponseEntity<ApiResponse<InquiryResponse>> update(
            @PathVariable Long id, @Valid @RequestBody InquiryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Inquiry updated", inquiryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft delete inquiry")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        inquiryService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Inquiry deleted"));
    }

    @GetMapping("/follow-ups/pending")
    @Operation(summary = "Get inquiries with pending follow-ups due today or earlier")
    public ResponseEntity<ApiResponse<List<InquiryResponse>>> getPendingFollowUps() {
        return ResponseEntity.ok(ApiResponse.success("Pending follow-ups", inquiryService.getPendingFollowUps()));
    }

    @GetMapping("/by-status")
    @Operation(summary = "Get inquiries by status")
    public ResponseEntity<ApiResponse<List<InquiryResponse>>> getByStatus(@RequestParam InquiryStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Inquiries by status", inquiryService.getByStatus(status)));
    }

    @PostMapping("/{id}/follow-ups")
    @Operation(summary = "Add a follow-up to an inquiry")
    public ResponseEntity<ApiResponse<FollowUpResponse>> addFollowUp(
            @PathVariable Long id, @Valid @RequestBody FollowUpRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Follow-up added", inquiryService.addFollowUp(id, request)));
    }

    @GetMapping("/{id}/follow-ups")
    @Operation(summary = "Get follow-ups for an inquiry")
    public ResponseEntity<ApiResponse<List<FollowUpResponse>>> getFollowUps(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Follow-ups fetched", inquiryService.getFollowUps(id)));
    }

    @PostMapping("/{id}/counseling-notes")
    @Operation(summary = "Add counseling note to inquiry")
    public ResponseEntity<ApiResponse<CounselingNoteResponse>> addCounselingNote(
            @PathVariable Long id, @Valid @RequestBody CounselingNoteRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Counseling note added", inquiryService.addCounselingNote(id, request)));
    }

    @GetMapping("/{id}/counseling-notes")
    @Operation(summary = "Get counseling notes for an inquiry")
    public ResponseEntity<ApiResponse<List<CounselingNoteResponse>>> getCounselingNotes(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Counseling notes fetched", inquiryService.getCounselingNotes(id)));
    }

    @PutMapping("/{id}/assign-counselor")
    @Operation(summary = "Assign a counselor to an inquiry")
    public ResponseEntity<ApiResponse<InquiryResponse>> assignCounselor(
            @PathVariable Long id, @RequestParam Long counselorId) {
        return ResponseEntity.ok(ApiResponse.success("Counselor assigned", inquiryService.assignCounselor(id, counselorId)));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update inquiry status")
    public ResponseEntity<ApiResponse<InquiryResponse>> updateStatus(
            @PathVariable Long id, @RequestParam InquiryStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", inquiryService.updateStatus(id, status)));
    }

    @GetMapping("/kpi")
    @Operation(summary = "Get admission KPI summary dashboard")
    public ResponseEntity<ApiResponse<AdmissionKpiResponse>> getKpi() {
        return ResponseEntity.ok(ApiResponse.success("Admission KPI", inquiryService.getKpi()));
    }
}
