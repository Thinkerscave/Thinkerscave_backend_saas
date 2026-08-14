package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.ClassSubjectMappingRequest;
import com.thinkerscave.academics.dto.request.SubjectRequest;
import com.thinkerscave.academics.dto.response.ClassMappingBoardResponse;
import com.thinkerscave.academics.dto.response.ClassSubjectMappingResponse;
import com.thinkerscave.academics.dto.response.SubjectResponse;
import com.thinkerscave.academics.dto.response.SubjectsMappingDashboardResponse;
import com.thinkerscave.academics.enums.SubjectCategory;
import com.thinkerscave.academics.service.SubjectService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academics")
@RequiredArgsConstructor
@Tag(name = "Subjects & Mapping", description = "Manage subjects and class-subject mapping")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping("/years/{yearId}/subjects/dashboard")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "Subjects & Mapping dashboard")
    public ResponseEntity<ApiResponse<SubjectsMappingDashboardResponse>> dashboard(
            @PathVariable Long yearId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SubjectCategory category,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(ApiResponse.success(
                "Subjects dashboard", subjectService.getDashboard(yearId, q, category, active)));
    }

    @GetMapping("/years/{yearId}/subjects")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "List subjects for academic year")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> byYear(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success("Subjects retrieved", subjectService.getByYear(yearId)));
    }

    @PostMapping("/subjects")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Create subject")
    public ResponseEntity<ApiResponse<SubjectResponse>> create(@Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Subject created", subjectService.create(request)));
    }

    @PutMapping("/subjects/{subjectId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Update subject")
    public ResponseEntity<ApiResponse<SubjectResponse>> update(
            @PathVariable Long subjectId, @Valid @RequestBody SubjectRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Subject updated", subjectService.update(subjectId, request)));
    }

    @GetMapping("/subjects/{subjectId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "Get subject by ID")
    public ResponseEntity<ApiResponse<SubjectResponse>> getById(@PathVariable Long subjectId) {
        return ResponseEntity.ok(ApiResponse.success("Subject found", subjectService.getById(subjectId)));
    }

    @PatchMapping("/subjects/{subjectId}/deactivate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Deactivate subject")
    public ResponseEntity<ApiResponse<SubjectResponse>> deactivate(@PathVariable Long subjectId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Subject deactivated", subjectService.deactivate(subjectId)));
    }

    @PatchMapping("/subjects/{subjectId}/activate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Activate subject")
    public ResponseEntity<ApiResponse<SubjectResponse>> activate(@PathVariable Long subjectId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Subject activated", subjectService.activate(subjectId)));
    }

    @GetMapping("/classes/{classId}/subject-mappings")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "Class subject mapping board")
    public ResponseEntity<ApiResponse<ClassMappingBoardResponse>> classMappingBoard(
            @PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Class mapping board", subjectService.getClassMappingBoard(classId)));
    }

    @PutMapping("/classes/{classId}/subject-mappings")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Include/update or exclude a subject for a class")
    public ResponseEntity<ApiResponse<ClassSubjectMappingResponse>> upsertMapping(
            @PathVariable Long classId, @Valid @RequestBody ClassSubjectMappingRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Class subject mapping saved", subjectService.upsertClassMapping(classId, request)));
    }
}
