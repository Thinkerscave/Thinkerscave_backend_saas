package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.SubjectMappingSaveRequest;
import com.thinkerscave.academics.dto.request.SubjectChoiceMappingSaveRequest;
import com.thinkerscave.academics.dto.response.AvailableSubjectResponse;
import com.thinkerscave.academics.dto.response.SubjectMappingBoardResponse;
import com.thinkerscave.academics.dto.response.SubjectMappingImpactResponse;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.academics.service.SubjectMappingService;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/academics/academic-groups/{academicGroupId}")
@RequiredArgsConstructor
@Tag(name = "Subject Mapping", description = "Academic Group curriculum mapping workspace")
public class SubjectMappingController {

    private final SubjectMappingService subjectMappingService;

    @GetMapping("/subject-mappings")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Curriculum board: active mappings, sections, and counts for an academic group")
    public ResponseEntity<ApiResponse<SubjectMappingBoardResponse>> board(@PathVariable Long academicGroupId) {
        return ResponseEntity.ok(ApiResponse.success("Subject mappings retrieved",
                subjectMappingService.getBoard(academicGroupId)));
    }

    @GetMapping("/available-subjects")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Catalog subjects offered for an academic group")
    public ResponseEntity<ApiResponse<PageResponse<AvailableSubjectResponse>>> availableSubjects(
            @PathVariable Long academicGroupId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) SubjectType subjectType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.success("Available subjects retrieved",
                subjectMappingService.getAvailableSubjects(academicGroupId, q, subjectType, page, size)));
    }

    @PostMapping("/subject-mappings/preview")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Validate a curriculum save and return its dependency impact without writing")
    public ResponseEntity<ApiResponse<SubjectMappingImpactResponse>> preview(
            @PathVariable Long academicGroupId, @Valid @RequestBody SubjectMappingSaveRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subject mapping preview",
                subjectMappingService.preview(academicGroupId, request)));
    }

    @PutMapping("/subject-mappings")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Save curriculum changes for an academic group in one transaction")
    public ResponseEntity<ApiResponse<SubjectMappingBoardResponse>> save(
            @PathVariable Long academicGroupId, @Valid @RequestBody SubjectMappingSaveRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subject mappings saved",
                subjectMappingService.save(academicGroupId, request)));
    }

    @PutMapping("/subject-mappings/choice-groups")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Map reusable choice groups and their subject alternatives to an academic group")
    public ResponseEntity<ApiResponse<SubjectMappingBoardResponse>> saveChoiceMappings(
            @PathVariable Long academicGroupId, @Valid @RequestBody SubjectChoiceMappingSaveRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Choice group mappings saved",
                subjectMappingService.saveChoiceMappings(academicGroupId, request)));
    }
}
