package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.AcademicGroupRequest;
import com.thinkerscave.academics.dto.request.AcademicSectionRequest;
import com.thinkerscave.academics.dto.response.AcademicGroupResponse;
import com.thinkerscave.academics.dto.response.AcademicGroupSummaryResponse;
import com.thinkerscave.academics.dto.response.AcademicSectionResponse;
import com.thinkerscave.academics.dto.response.AcademicStructureTreeResponse;
import com.thinkerscave.academics.dto.response.AcademicGroupsDashboardResponse;
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
@Tag(name = "Academic Groups", description = "Manage Academic Groups and sections")
public class AcademicStructureController {

    private final AcademicStructureService structureService;

    @GetMapping("/years/{yearId}/academic-groups/dashboard")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Academic Groups dashboard for an academic year")
    public ResponseEntity<ApiResponse<AcademicGroupsDashboardResponse>> dashboard(
            @PathVariable Long yearId,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) AcademicStage stage,
            @RequestParam(required = false) Boolean active) {
        return ResponseEntity.ok(ApiResponse.success(
                "Academic Groups dashboard", structureService.getDashboard(yearId, q, stage, active)));
    }

    @PostMapping("/academic-groups")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Create Academic Group")
    public ResponseEntity<ApiResponse<AcademicGroupResponse>> createAcademicGroup(
            @Valid @RequestBody AcademicGroupRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Academic Group created", structureService.createAcademicGroup(request)));
    }

    @PutMapping("/academic-groups/{academicGroupId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Update Academic Group")
    public ResponseEntity<ApiResponse<AcademicGroupResponse>> updateAcademicGroup(
            @PathVariable Long academicGroupId, @Valid @RequestBody AcademicGroupRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Academic Group updated", structureService.updateAcademicGroup(academicGroupId, request)));
    }

    @GetMapping("/academic-groups/{academicGroupId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Get Academic Group by ID")
    public ResponseEntity<ApiResponse<AcademicGroupResponse>> getAcademicGroupById(@PathVariable Long academicGroupId) {
        return ResponseEntity.ok(ApiResponse.success("Academic Group found", structureService.getAcademicGroupById(academicGroupId)));
    }

    @GetMapping("/years/{yearId}/academic-groups")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Get Academic Groups by academic year")
    public ResponseEntity<ApiResponse<List<AcademicGroupResponse>>> getAcademicGroups(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success("Academic Groups retrieved", structureService.getAcademicGroupsByYear(yearId)));
    }

    @PatchMapping("/academic-groups/{academicGroupId}/deactivate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Deactivate Academic Group (is_active = false)")
    public ResponseEntity<ApiResponse<AcademicGroupResponse>> deactivateAcademicGroup(@PathVariable Long academicGroupId) {
        return ResponseEntity.ok(ApiResponse.success("Academic Group deactivated", structureService.deactivateAcademicGroup(academicGroupId)));
    }

    @PatchMapping("/academic-groups/{academicGroupId}/activate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Activate Academic Group (is_active = true)")
    public ResponseEntity<ApiResponse<AcademicGroupResponse>> activateAcademicGroup(@PathVariable Long academicGroupId) {
        return ResponseEntity.ok(ApiResponse.success("Academic Group activated", structureService.activateAcademicGroup(academicGroupId)));
    }

    @PostMapping("/academic-groups/{academicGroupId}/sections")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Create section")
    public ResponseEntity<ApiResponse<AcademicSectionResponse>> createSection(
            @PathVariable Long academicGroupId, @Valid @RequestBody AcademicSectionRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.success("Section created", structureService.createSection(academicGroupId, request)));
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
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Get section by ID")
    public ResponseEntity<ApiResponse<AcademicSectionResponse>> getSectionById(@PathVariable Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success("Section found", structureService.getSectionById(sectionId)));
    }

    @GetMapping("/academic-groups/{academicGroupId}/sections")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Get sections by Academic Group")
    public ResponseEntity<ApiResponse<List<AcademicSectionResponse>>> getSections(@PathVariable Long academicGroupId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Sections retrieved", structureService.getSectionsByAcademicGroup(academicGroupId)));
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

    @GetMapping("/academic-groups/{academicGroupId}/summary")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Live counts for one academic group (sections, students, subjects, teachers, timetable)")
    public ResponseEntity<ApiResponse<AcademicGroupSummaryResponse>> groupSummary(@PathVariable Long academicGroupId) {
        return ResponseEntity.ok(ApiResponse.success("Group summary", structureService.getGroupSummary(academicGroupId)));
    }

    @GetMapping("/years/{yearId}/structure")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Get full academic structure tree for a year")
    public ResponseEntity<ApiResponse<List<AcademicStructureTreeResponse>>> getTree(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Structure retrieved", structureService.getStructureTree(yearId)));
    }
}
