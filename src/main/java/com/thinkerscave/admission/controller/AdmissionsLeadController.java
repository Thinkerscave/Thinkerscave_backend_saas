package com.thinkerscave.admission.controller;

import com.thinkerscave.admission.dto.request.AssignCounselorRequest;
import com.thinkerscave.admission.dto.request.InquiryRequest;
import com.thinkerscave.admission.dto.request.LeadSearchRequest;
import com.thinkerscave.admission.dto.request.MarkLostRequest;
import com.thinkerscave.admission.dto.response.ApplicationAdmissionResponse;
import com.thinkerscave.admission.dto.response.InquiryResponse;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admissions/leads")
@RequiredArgsConstructor
@Tag(name = "Admissions CRM - Leads")
public class AdmissionsLeadController {

    private final InquiryService inquiryService;

    @PostMapping
    @Operation(summary = "Create lead")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<InquiryResponse>> create(@Valid @RequestBody InquiryRequest request) {
        return ResponseEntity.ok(ApiResponse.created("Lead created", inquiryService.create(request)));
    }

    @GetMapping
    @Operation(summary = "List leads")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<Page<InquiryResponse>>> list(Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Leads loaded", inquiryService.getAll(pageable)));
    }

    @PostMapping("/search")
    @Operation(summary = "Search leads")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<Page<InquiryResponse>>> search(
            @RequestBody(required = false) LeadSearchRequest request,
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success("Leads search completed", inquiryService.search(request, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lead detail")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<InquiryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lead loaded", inquiryService.getById(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update lead")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<InquiryResponse>> update(@PathVariable Long id, @Valid @RequestBody InquiryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Lead updated", inquiryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Archive lead")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<Void>> archive(@PathVariable Long id) {
        inquiryService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.noContent("Lead archived"));
    }

    @PostMapping("/{id}/assign-counselor")
    @Operation(summary = "Assign counselor")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<InquiryResponse>> assignCounselor(
            @PathVariable Long id,
            @Valid @RequestBody AssignCounselorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Counselor assigned",
                inquiryService.assignCounselor(id, request.getCounselorId())));
    }

    @PostMapping("/{id}/mark-lost")
    @Operation(summary = "Mark lead as lost")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<InquiryResponse>> markLost(
            @PathVariable Long id,
            @Valid @RequestBody MarkLostRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Lead marked as lost", inquiryService.markLost(id, request.getReason())));
    }

    @PostMapping("/{id}/convert-to-application")
    @Operation(summary = "Convert lead to application")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<ApplicationAdmissionResponse>> convertToApplication(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lead converted to application", inquiryService.convertToApplication(id)));
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Update lead status")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<InquiryResponse>> updateStatus(@PathVariable Long id,
                                                                      @RequestParam com.thinkerscave.admission.enums.InquiryStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Lead status updated", inquiryService.updateStatus(id, status)));
    }
}