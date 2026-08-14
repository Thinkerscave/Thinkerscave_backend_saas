package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.AcademicResourceRequest;
import com.thinkerscave.academics.dto.request.TimetableConfigurationRequest;
import com.thinkerscave.academics.dto.request.TimetableGenerationStartRequest;
import com.thinkerscave.academics.dto.response.*;
import com.thinkerscave.academics.service.TimetableService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/academics")
@RequiredArgsConstructor
@Tag(name = "Timetable", description = "Timetable configuration, generation, and lifecycle management")
public class TimetableController {

    private final TimetableService timetableService;

    // ─── Dashboard ────────────────────────────────────────────────────────

    @GetMapping("/years/{yearId}/timetable/dashboard")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "Timetable dashboard with readiness checks")
    public ResponseEntity<ApiResponse<TimetableDashboardResponse>> dashboard(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Timetable dashboard", timetableService.getDashboard(yearId)));
    }

    // ─── Readiness ────────────────────────────────────────────────────────

    @GetMapping("/years/{yearId}/timetable/readiness")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "Evaluate timetable generation readiness")
    public ResponseEntity<ApiResponse<TimetableReadinessResponse>> readiness(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Timetable readiness", timetableService.evaluateReadiness(yearId)));
    }

    // ─── Configuration ────────────────────────────────────────────────────

    @GetMapping("/years/{yearId}/timetable/configuration")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "Get timetable configuration for year")
    public ResponseEntity<ApiResponse<TimetableConfigurationResponse>> getConfiguration(
            @PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Timetable configuration", timetableService.getConfiguration(yearId)));
    }

    @PutMapping("/years/{yearId}/timetable/configuration")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Upsert timetable configuration for year")
    public ResponseEntity<ApiResponse<TimetableConfigurationResponse>> upsertConfiguration(
            @PathVariable Long yearId,
            @Valid @RequestBody TimetableConfigurationRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Timetable configuration saved", timetableService.upsertConfiguration(yearId, request)));
    }

    // ─── Resources ────────────────────────────────────────────────────────

    @GetMapping("/resources")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "List active academic resources")
    public ResponseEntity<ApiResponse<List<AcademicResourceResponse>>> listResources() {
        return ResponseEntity.ok(ApiResponse.success(
                "Academic resources", timetableService.listResources()));
    }

    @PostMapping("/resources")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Create academic resource")
    public ResponseEntity<ApiResponse<AcademicResourceResponse>> createResource(
            @Valid @RequestBody AcademicResourceRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.created(
                "Resource created", timetableService.createResource(request)));
    }

    @PutMapping("/resources/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Update academic resource")
    public ResponseEntity<ApiResponse<AcademicResourceResponse>> updateResource(
            @PathVariable Long id,
            @Valid @RequestBody AcademicResourceRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Resource updated", timetableService.updateResource(id, request)));
    }

    @PostMapping("/resources/{id}/deactivate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Deactivate academic resource")
    public ResponseEntity<ApiResponse<AcademicResourceResponse>> deactivateResource(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(
                "Resource deactivated", timetableService.deactivateResource(id)));
    }

    // ─── Generation ───────────────────────────────────────────────────────

    @PostMapping("/years/{yearId}/timetable/generations")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Start asynchronous timetable generation")
    public ResponseEntity<ApiResponse<TimetableGenerationAcceptedResponse>> startGeneration(
            @PathVariable Long yearId,
            @RequestBody(required = false) TimetableGenerationStartRequest request) {
        TimetableGenerationAcceptedResponse response = timetableService.startGeneration(yearId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(
                "Timetable generation started", response));
    }

    @GetMapping("/timetable/generations/{generationId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "Get timetable generation progress")
    public ResponseEntity<ApiResponse<TimetableGenerationProgressResponse>> generationProgress(
            @PathVariable Long generationId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Generation progress", timetableService.getGenerationProgress(generationId)));
    }

    @PostMapping("/timetable/generations/{generationId}/cancel")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Cancel active timetable generation")
    public ResponseEntity<ApiResponse<Void>> cancelGeneration(@PathVariable Long generationId) {
        timetableService.cancelGeneration(generationId);
        return ResponseEntity.ok(ApiResponse.success("Generation cancel requested", null));
    }

    @PostMapping("/years/{yearId}/timetable/generate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Generate timetable (deprecated - use POST .../generations)")
    @Deprecated
    public ResponseEntity<ApiResponse<TimetableGenerateResultResponse>> generate(@PathVariable Long yearId) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(
                "Timetable generation started", timetableService.generate(yearId)));
    }

    // ─── Grid ─────────────────────────────────────────────────────────────

    @GetMapping("/timetable/versions/{versionId}/grid")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "Get timetable grid for a version")
    public ResponseEntity<ApiResponse<TimetableGridResponse>> getGrid(
            @PathVariable Long versionId,
            @RequestParam(defaultValue = "CLASS") String view,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) Long resourceId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Timetable grid", timetableService.getGrid(versionId, view, sectionId, staffId, resourceId)));
    }

    // ─── Conflicts ────────────────────────────────────────────────────────

    @GetMapping("/timetable/versions/{versionId}/conflicts")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "List conflicts for a version")
    public ResponseEntity<ApiResponse<List<TimetableConflictResponse>>> getConflicts(
            @PathVariable Long versionId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Timetable conflicts", timetableService.getConflicts(versionId)));
    }

    @PostMapping("/timetable/conflicts/{conflictId}/resolve")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Resolve a timetable conflict")
    public ResponseEntity<ApiResponse<TimetableConflictResponse>> resolveConflict(
            @PathVariable Long conflictId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Conflict resolved", timetableService.resolveConflict(conflictId)));
    }

    @PostMapping("/timetable/conflicts/{conflictId}/ignore")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Ignore a timetable conflict")
    public ResponseEntity<ApiResponse<TimetableConflictResponse>> ignoreConflict(
            @PathVariable Long conflictId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Conflict ignored", timetableService.ignoreConflict(conflictId)));
    }

    // ─── Versions ─────────────────────────────────────────────────────────

    @GetMapping("/years/{yearId}/timetable/versions")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "List timetable versions for year")
    public ResponseEntity<ApiResponse<List<TimetableVersionResponse>>> listVersions(
            @PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Timetable versions", timetableService.listVersions(yearId)));
    }

    // ─── Lifecycle ────────────────────────────────────────────────────────

    @PostMapping("/timetable/versions/{versionId}/submit")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Submit timetable version for approval")
    public ResponseEntity<ApiResponse<TimetableVersionResponse>> submit(@PathVariable Long versionId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Version submitted", timetableService.submitVersion(versionId)));
    }

    @PostMapping("/timetable/versions/{versionId}/approve")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Approve timetable version")
    public ResponseEntity<ApiResponse<TimetableVersionResponse>> approve(@PathVariable Long versionId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Version approved", timetableService.approveVersion(versionId)));
    }

    @PostMapping("/timetable/versions/{versionId}/reject")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Reject timetable version")
    public ResponseEntity<ApiResponse<TimetableVersionResponse>> reject(@PathVariable Long versionId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Version rejected", timetableService.rejectVersion(versionId)));
    }

    @PostMapping("/timetable/versions/{versionId}/publish")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Publish timetable version")
    public ResponseEntity<ApiResponse<TimetableVersionResponse>> publish(@PathVariable Long versionId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Version published", timetableService.publishVersion(versionId)));
    }
}
