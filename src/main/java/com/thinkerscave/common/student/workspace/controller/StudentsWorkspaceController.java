package com.thinkerscave.common.student.workspace.controller;

import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.student.workspace.dto.*;
import com.thinkerscave.common.student.workspace.service.StudentsWorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/students/workspace")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'PRINCIPAL', 'TEACHER', 'STAFF', 'RECEPTIONIST')")
public class StudentsWorkspaceController {

    private final StudentsWorkspaceService service;

    // ---------- KPI ----------
    @GetMapping("/kpi")
    public ResponseEntity<ApiResponse<StudentKpiResponse>> kpi() {
        return ResponseEntity.ok(ApiResponse.success(service.kpi()));
    }

    // ---------- Directory ----------
    @PostMapping("/directory/search")
    public ResponseEntity<ApiResponse<org.springframework.data.domain.Page<StudentDirectoryCard>>> directorySearch(
            @RequestBody(required = false) StudentSearchRequest req,
            org.springframework.data.domain.Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(service.directorySearch(req, pageable)));
    }

    // ---------- Profile 360 ----------
    @GetMapping("/students/{studentId}/profile-360")
    public ResponseEntity<ApiResponse<StudentProfile360Response>> profile360(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(service.profile360(studentId)));
    }

    @GetMapping("/students/{studentId}/timeline")
    public ResponseEntity<ApiResponse<List<StudentTimelineEntry>>> timeline(@PathVariable Long studentId) {
        return ResponseEntity.ok(ApiResponse.success(service.timeline(studentId)));
    }

    // ---------- Alumni ----------
    @GetMapping("/alumni")
    public ResponseEntity<ApiResponse<List<AlumniResponse>>> alumni() {
        return ResponseEntity.ok(ApiResponse.success(service.alumniList()));
    }

    // ---------- Document Vault ----------
    @GetMapping("/documents/kpi")
    public ResponseEntity<ApiResponse<DocumentVaultKpi>> documentsKpi() {
        return ResponseEntity.ok(ApiResponse.success(service.documentVaultKpi()));
    }

    @GetMapping("/documents")
    public ResponseEntity<ApiResponse<List<DocumentVaultEntry>>> documents(@RequestParam(required = false) String category) {
        return ResponseEntity.ok(ApiResponse.success(service.documentVaultList(category)));
    }

    @PostMapping("/documents")
    public ResponseEntity<ApiResponse<DocumentVaultEntry>> addDocument(@Valid @RequestBody DocumentVaultRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Document uploaded", service.addDocument(req)));
    }

    @PostMapping("/documents/{id}/verify")
    public ResponseEntity<ApiResponse<DocumentVaultEntry>> verifyDocument(@PathVariable Long id, Authentication auth) {
        String verifier = auth != null ? auth.getName() : "SYSTEM";
        return ResponseEntity.ok(ApiResponse.success("Document verified", service.verifyDocument(id, verifier)));
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable Long id) {
        service.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success("Document deleted", null));
    }
}
