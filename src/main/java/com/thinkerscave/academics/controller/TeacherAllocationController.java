package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.ClassTeacherAssignmentRequest;
import com.thinkerscave.academics.dto.request.TeacherAllocationAssignRequest;
import com.thinkerscave.academics.dto.response.ClassTeacherAssignmentResponse;
import com.thinkerscave.academics.dto.response.TeacherAllocationDashboardResponse;
import com.thinkerscave.academics.dto.response.TeacherAllocationRowResponse;
import com.thinkerscave.academics.dto.response.TeacherRecommendationResponse;
import com.thinkerscave.academics.dto.response.TeacherWorkloadResponse;
import com.thinkerscave.academics.enums.TeacherAllocationStatus;
import com.thinkerscave.academics.service.TeacherAllocationService;
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
@Tag(name = "Teacher Allocation", description = "Assign teachers to class-section-subject slots")
public class TeacherAllocationController {

    private final TeacherAllocationService allocationService;

    @GetMapping("/years/{yearId}/teacher-allocations/dashboard")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "Teacher Allocation dashboard")
    public ResponseEntity<ApiResponse<TeacherAllocationDashboardResponse>> dashboard(
            @PathVariable Long yearId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) TeacherAllocationStatus status) {
        return ResponseEntity.ok(ApiResponse.success(
                "Teacher allocation dashboard",
                allocationService.getDashboard(yearId, classId, sectionId, subjectId, status)));
    }

    @PostMapping("/teacher-allocations/assign")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Assign teacher to a section subject slot")
    public ResponseEntity<ApiResponse<TeacherAllocationRowResponse>> assign(
            @Valid @RequestBody TeacherAllocationAssignRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Teacher assigned", allocationService.assign(request)));
    }

    @PostMapping("/teacher-allocations/{allocationId}/unassign")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Unassign teachers from a slot")
    public ResponseEntity<ApiResponse<TeacherAllocationRowResponse>> unassign(@PathVariable Long allocationId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Teacher unassigned", allocationService.unassign(allocationId)));
    }

    @GetMapping("/teacher-allocations/recommendations")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Recommended teachers for a slot")
    public ResponseEntity<ApiResponse<List<TeacherRecommendationResponse>>> recommendations(
            @RequestParam Long sectionId,
            @RequestParam Long classSubjectMappingId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Recommendations",
                allocationService.recommendations(sectionId, classSubjectMappingId)));
    }

    @GetMapping("/years/{yearId}/teacher-workloads")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Teacher workloads for academic year")
    public ResponseEntity<ApiResponse<List<TeacherWorkloadResponse>>> workloads(@PathVariable Long yearId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Teacher workloads", allocationService.listWorkloads(yearId)));
    }

    @GetMapping("/allocations/workload")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Get one teacher workload")
    public ResponseEntity<ApiResponse<TeacherWorkloadResponse>> getWorkload(
            @RequestParam Long teacherId,
            @RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Teacher workload retrieved",
                allocationService.getTeacherWorkload(teacherId, academicYearId)));
    }

    @PostMapping("/allocations/class-teachers")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Assign class teacher")
    public ResponseEntity<ApiResponse<ClassTeacherAssignmentResponse>> assignClassTeacher(
            @Valid @RequestBody ClassTeacherAssignmentRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(
                "Class teacher assigned", allocationService.assignClassTeacher(request)));
    }

    @GetMapping("/allocations/class-teachers")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "List class teacher assignments")
    public ResponseEntity<ApiResponse<List<ClassTeacherAssignmentResponse>>> getClassTeachers(
            @RequestParam Long yearId,
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Assignments retrieved",
                allocationService.getClassTeachers(yearId, classId, sectionId)));
    }

    @DeleteMapping("/allocations/class-teachers/{assignmentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Remove class teacher assignment")
    public ResponseEntity<ApiResponse<Void>> removeClassTeacher(@PathVariable Long assignmentId) {
        allocationService.removeClassTeacher(assignmentId);
        return ResponseEntity.ok(ApiResponse.success("Class teacher assignment removed", null));
    }
}
