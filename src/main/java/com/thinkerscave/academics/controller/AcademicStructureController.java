package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.AcademicClassRequest;
import com.thinkerscave.academics.dto.request.AcademicSectionRequest;
import com.thinkerscave.academics.dto.response.AcademicClassResponse;
import com.thinkerscave.academics.dto.response.AcademicSectionResponse;
import com.thinkerscave.academics.dto.response.AcademicStructureTreeResponse;
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
@Tag(name = "Academic Structure", description = "Manage academic classes and sections")
public class AcademicStructureController {

    private final AcademicStructureService structureService;

    // ---- Class ----

    @PostMapping("/classes")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Create class")
    public ResponseEntity<ApiResponse<AcademicClassResponse>> createClass(@Valid @RequestBody AcademicClassRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Class created", structureService.createClass(request)));
    }

    @PutMapping("/classes/{classId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Update class")
    public ResponseEntity<ApiResponse<AcademicClassResponse>> updateClass(
            @PathVariable Long classId, @Valid @RequestBody AcademicClassRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Class updated", structureService.updateClass(classId, request)));
    }

    @GetMapping("/classes/{classId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get class by ID")
    public ResponseEntity<ApiResponse<AcademicClassResponse>> getClassById(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.success("Class found", structureService.getClassById(classId)));
    }

    @GetMapping("/years/{yearId}/classes")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get classes by academic year")
    public ResponseEntity<ApiResponse<List<AcademicClassResponse>>> getClasses(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success("Classes retrieved", structureService.getClassesByYear(yearId)));
    }

    @PatchMapping("/classes/{classId}/deactivate")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Deactivate class")
    public ResponseEntity<ApiResponse<Void>> deactivateClass(@PathVariable Long classId) {
        structureService.deactivateClass(classId);
        return ResponseEntity.ok(ApiResponse.success("Class deactivated", null));
    }

    // ---- Section ----

    @PostMapping("/classes/{classId}/sections")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Create section")
    public ResponseEntity<ApiResponse<AcademicSectionResponse>> createSection(
            @PathVariable Long classId, @Valid @RequestBody AcademicSectionRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Section created", structureService.createSection(classId, request)));
    }

    @PutMapping("/sections/{sectionId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN')")
    @Operation(summary = "Update section")
    public ResponseEntity<ApiResponse<AcademicSectionResponse>> updateSection(
            @PathVariable Long sectionId, @Valid @RequestBody AcademicSectionRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Section updated", structureService.updateSection(sectionId, request)));
    }

    @GetMapping("/sections/{sectionId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get section by ID")
    public ResponseEntity<ApiResponse<AcademicSectionResponse>> getSectionById(@PathVariable Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success("Section found", structureService.getSectionById(sectionId)));
    }

    @GetMapping("/classes/{classId}/sections")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get sections by class")
    public ResponseEntity<ApiResponse<List<AcademicSectionResponse>>> getSections(@PathVariable Long classId) {
        return ResponseEntity.ok(ApiResponse.success("Sections retrieved", structureService.getSectionsByClass(classId)));
    }

    @PatchMapping("/sections/{sectionId}/deactivate")
    @PreAuthorize("hasAuthority('SUPER_ADMIN')")
    @Operation(summary = "Deactivate section")
    public ResponseEntity<ApiResponse<Void>> deactivateSection(@PathVariable Long sectionId) {
        structureService.deactivateSection(sectionId);
        return ResponseEntity.ok(ApiResponse.success("Section deactivated", null));
    }

    // ---- Tree ----

    @GetMapping("/years/{yearId}/structure")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','ACADEMIC_COORDINATOR','TEACHER')")
    @Operation(summary = "Get full academic structure tree for a year")
    public ResponseEntity<ApiResponse<List<AcademicStructureTreeResponse>>> getTree(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success("Structure retrieved", structureService.getStructureTree(yearId)));
    }
}
