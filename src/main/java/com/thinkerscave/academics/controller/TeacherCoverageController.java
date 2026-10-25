package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.TeacherCoverageSaveRequest;
import com.thinkerscave.academics.dto.response.TeacherCandidateResponse;
import com.thinkerscave.academics.dto.response.TeacherCoverageBoardResponse;
import com.thinkerscave.academics.dto.response.TeacherCoveragePreviewResponse;
import com.thinkerscave.academics.service.TeacherCoverageService;
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
@RequestMapping("/api/v1/academics/years/{academicYearId}/teacher-coverage")
@RequiredArgsConstructor
@Tag(name = "Teacher Allocation", description = "Academic Group teacher defaults and section overrides")
public class TeacherCoverageController {

    private final TeacherCoverageService teacherCoverageService;

    @GetMapping("/board")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Teacher allocation board for an academic group")
    public ResponseEntity<ApiResponse<TeacherCoverageBoardResponse>> board(
            @PathVariable Long academicYearId, @RequestParam Long academicGroupId) {
        return ResponseEntity.ok(ApiResponse.success("Teacher allocation retrieved",
                teacherCoverageService.getBoard(academicYearId, academicGroupId)));
    }

    @GetMapping("/candidates")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Ranked teacher candidates for a subject, with projected workload")
    public ResponseEntity<ApiResponse<List<TeacherCandidateResponse>>> candidates(
            @PathVariable Long academicYearId,
            @RequestParam Long academicGroupId,
            @RequestParam Long mappingId,
            @RequestParam(required = false) Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success("Teacher candidates retrieved",
                teacherCoverageService.getCandidates(academicYearId, academicGroupId, mappingId, sectionId)));
    }

    @PostMapping("/preview")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Preview affected sections, workload warnings, and impact without saving")
    public ResponseEntity<ApiResponse<TeacherCoveragePreviewResponse>> preview(
            @PathVariable Long academicYearId, @Valid @RequestBody TeacherCoverageSaveRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Teacher allocation preview",
                teacherCoverageService.preview(academicYearId, request)));
    }

    @PutMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Save teacher allocation changes for an academic group")
    public ResponseEntity<ApiResponse<TeacherCoverageBoardResponse>> save(
            @PathVariable Long academicYearId, @Valid @RequestBody TeacherCoverageSaveRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Teacher allocation saved",
                teacherCoverageService.save(academicYearId, request)));
    }
}
