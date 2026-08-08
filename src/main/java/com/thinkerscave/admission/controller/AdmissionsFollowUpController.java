package com.thinkerscave.admission.controller;

import com.thinkerscave.admission.dto.request.FollowUpRequest;
import com.thinkerscave.admission.dto.response.FollowUpResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admissions/follow-ups")
@RequiredArgsConstructor
@Tag(name = "Admissions CRM - Follow-ups")
public class AdmissionsFollowUpController {

    private final InquiryService inquiryService;

    @GetMapping("/today")
    @Operation(summary = "Get today's follow-ups")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<List<FollowUpResponse>>> today() {
        return ResponseEntity.ok(ApiResponse.success("Today's follow-ups", inquiryService.getTodayFollowUps()));
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue follow-ups")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<List<FollowUpResponse>>> overdue() {
        return ResponseEntity.ok(ApiResponse.success("Overdue follow-ups", inquiryService.getOverdueFollowUps()));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update follow-up")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<FollowUpResponse>> update(@PathVariable Long id, @Valid @RequestBody FollowUpRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Follow-up updated", inquiryService.updateFollowUp(id, request)));
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete follow-up")
    @PreAuthorize("hasAnyAuthority('ORGANIZATION_ADMIN','ORGANIZATION_OWNER','TEACHER')")
    public ResponseEntity<ApiResponse<FollowUpResponse>> complete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Follow-up completed", inquiryService.completeFollowUp(id)));
    }
}