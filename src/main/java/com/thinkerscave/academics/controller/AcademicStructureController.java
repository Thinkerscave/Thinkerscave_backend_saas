package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.AcademicClassRequest;
import com.thinkerscave.academics.dto.request.AcademicSectionRequest;
import com.thinkerscave.academics.dto.response.AcademicClassResponse;
import com.thinkerscave.academics.dto.response.AcademicSectionResponse;
import com.thinkerscave.academics.dto.response.AcademicStructureTreeResponse;
import com.thinkerscave.academics.dto.response.ClassesSectionsDashboardResponse;
import com.thinkerscave.academics.enums.AcademicStage;
import com.thinkerscave.academics.service.AcademicStructureService;
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
@Tag(name = "Classes & Sections", description = "Manage academic classes and sections")
public class AcademicStructureController {

    private final AcademicStructureService structureService;

    @GetMapping("/years/{yearId}/classes/dashboard")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "Classes & Sections page dashboard for an academic year")
    public ResponseEntity<ApiResponse<ClassesSectionsDashboardResponse>> dashboard(
            @PathVariable Long yearId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) AcademicStage stage,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(ApiResponse.success(
                "Classes dashboard", structureService.getDashboard(yearId, q, stage, active)));
    }

    @PostMapping("/classes")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Create class")
    public ResponseEntity<ApiResponse<AcademicClassResponse>> createClass(
            @Valid @RequestBody AcademicClassRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Class created", structureService.createClass(request)));
    }

    @PutMapping("/classes/{classId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Update class")
    public ResponseEntity<ApiResponse<AcademicClassResponse>> updateClass(
            @PathVariable Long classId, @Valid @RequestBody AcademicClassRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Class updated", structureService.updateClass(classId, request)));
    }

    @GetMapping("/classes/{classId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "Get class by ID")
    public ResponseEntity<ApiResponse<AcademicClassResponse>> getClassById(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.success("Class found", structureService.getClassById(classId)));
    }

    @GetMapping("/years/{yearId}/classes")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "Get classes by academic year")
    public ResponseEntity<ApiResponse<List<AcademicClassResponse>>> getClasses(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success("Classes retrieved", structureService.getClassesByYear(yearId)));
    }

    @PatchMapping("/classes/{classId}/deactivate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Deactivate class (is_active = false)")
    public ResponseEntity<ApiResponse<AcademicClassResponse>> deactivateClass(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.success("Class deactivated", structureService.deactivateClass(classId)));
    }

    @PatchMapping("/classes/{classId}/activate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Activate class (is_active = true)")
    public ResponseEntity<ApiResponse<AcademicClassResponse>> activateClass(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.success("Class activated", structureService.activateClass(classId)));
    }

    @PostMapping("/classes/{classId}/sections")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Create section")
    public ResponseEntity<ApiResponse<AcademicSectionResponse>> createSection(
            @PathVariable Long classId, @Valid @RequestBody AcademicSectionRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Section created", structureService.createSection(classId, request)));
    }

    @PutMapping("/sections/{sectionId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Update section")
    public ResponseEntity<ApiResponse<AcademicSectionResponse>> updateSection(
            @PathVariable Long sectionId, @Valid @RequestBody AcademicSectionRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Section updated", structureService.updateSection(sectionId, request)));
    }

    @GetMapping("/sections/{sectionId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "Get section by ID")
    public ResponseEntity<ApiResponse<AcademicSectionResponse>> getSectionById(@PathVariable Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success("Section found", structureService.getSectionById(sectionId)));
    }

    @GetMapping("/classes/{classId}/sections")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "Get sections by class")
    public ResponseEntity<ApiResponse<List<AcademicSectionResponse>>> getSections(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sections retrieved", structureService.getSectionsByClass(classId)));
    }

    @PatchMapping("/sections/{sectionId}/deactivate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Deactivate section (is_active = false)")
    public ResponseEntity<ApiResponse<AcademicSectionResponse>> deactivateSection(@PathVariable Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Section deactivated", structureService.deactivateSection(sectionId)));
    }

    @PatchMapping("/sections/{sectionId}/activate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Activate section (is_active = true)")
    public ResponseEntity<ApiResponse<AcademicSectionResponse>> activateSection(@PathVariable Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Section activated", structureService.activateSection(sectionId)));
    }

    @GetMapping("/years/{yearId}/structure")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "Get full academic structure tree for a year")
    public ResponseEntity<ApiResponse<List<AcademicStructureTreeResponse>>> getTree(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Structure retrieved", structureService.getStructureTree(yearId)));
    }
}
