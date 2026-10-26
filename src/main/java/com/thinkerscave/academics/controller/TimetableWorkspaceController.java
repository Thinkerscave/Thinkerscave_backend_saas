package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.TimetableEntryRequest;
import com.thinkerscave.academics.dto.response.TimetableCellResponse;
import com.thinkerscave.academics.dto.response.TimetableConflictResponse;
import com.thinkerscave.academics.dto.response.TimetableGridResponse;
import com.thinkerscave.academics.dto.response.TimetableWorkspaceResponse;
import com.thinkerscave.academics.service.TimetableWorkspaceService;
import com.thinkerscave.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/academics")
@RequiredArgsConstructor
@Tag(name = "Timetable Workspace", description = "State-aware timetable workspace, views and manual editing")
public class TimetableWorkspaceController {

    private final TimetableWorkspaceService service;

    @GetMapping("/years/{yearId}/timetable/workspace")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "State-aware timetable workspace")
    public ResponseEntity<ApiResponse<TimetableWorkspaceResponse>> workspace(
            @PathVariable Long yearId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success("Timetable workspace",
                service.getWorkspace(yearId, date)));
    }

    @GetMapping("/timetable/versions/{versionId}/view")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Timetable grid for class or teacher view")
    public ResponseEntity<ApiResponse<TimetableGridResponse>> view(
            @PathVariable Long versionId,
            @RequestParam(defaultValue = "ACADEMIC_GROUP") String view,
            @RequestParam(required = false) Long academicGroupId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long staffId,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStartDate) {
        return ResponseEntity.ok(ApiResponse.success("Timetable view",
                service.getView(versionId, view, academicGroupId, sectionId, staffId, resourceId, weekStartDate)));
    }

    @PostMapping("/timetable/versions/{versionId}/entries")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Place a lesson in a draft timetable")
    public ResponseEntity<ApiResponse<TimetableCellResponse>> addEntry(
            @PathVariable Long versionId, @RequestBody TimetableEntryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(
                "Lesson placed", service.addEntry(versionId, request)));
    }

    @PutMapping("/timetable/entries/{entryId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Move, replace the teacher, or lock a lesson")
    public ResponseEntity<ApiResponse<TimetableCellResponse>> updateEntry(
            @PathVariable Long entryId, @RequestBody TimetableEntryRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Lesson updated",
                service.updateEntry(entryId, request)));
    }

    @DeleteMapping("/timetable/entries/{entryId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Remove a lesson from a draft timetable")
    public ResponseEntity<ApiResponse<Void>> deleteEntry(@PathVariable Long entryId) {
        service.deleteEntry(entryId);
        return ResponseEntity.ok(ApiResponse.success("Lesson removed", null));
    }

    @PostMapping("/timetable/entries/{entryId}/lock")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<TimetableCellResponse>> lockEntry(@PathVariable Long entryId) {
        return ResponseEntity.ok(ApiResponse.success("Lesson locked",
                service.setEntryLocked(entryId, true)));
    }

    @PostMapping("/timetable/entries/{entryId}/unlock")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    public ResponseEntity<ApiResponse<TimetableCellResponse>> unlockEntry(@PathVariable Long entryId) {
        return ResponseEntity.ok(ApiResponse.success("Lesson unlocked",
                service.setEntryLocked(entryId, false)));
    }

    @PostMapping("/timetable/versions/{versionId}/validate")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Re-validate a draft timetable")
    public ResponseEntity<ApiResponse<List<TimetableConflictResponse>>> validate(@PathVariable Long versionId) {
        return ResponseEntity.ok(ApiResponse.success("Timetable validated",
                service.validateVersion(versionId)));
    }
}
