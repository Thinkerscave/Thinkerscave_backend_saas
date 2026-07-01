package com.thinkerscave.academics.controller;

import com.thinkerscave.academics.dto.request.ClassTeacherAssignmentRequest;
import com.thinkerscave.academics.dto.request.SubjectAssignmentRequest;
import com.thinkerscave.academics.dto.response.ClassTeacherAssignmentResponse;
import com.thinkerscave.academics.dto.response.SubjectAssignmentResponse;
import com.thinkerscave.academics.dto.response.TeacherWorkloadResponse;
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
@RequestMapping("/api/v1/academics/allocations")
@RequiredArgsConstructor
@Tag(name = "Teacher Allocation", description = "Manage class teacher and subject assignments")
public class TeacherAllocationController {

    private final TeacherAllocationService allocationService;

    // ---- Class Teacher ----

    @PostMapping("/class-teachers")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Assign class teacher")
    public ResponseEntity<ApiResponse<ClassTeacherAssignmentResponse>> assignClassTeacher(
            @Valid @RequestBody ClassTeacherAssignmentRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Class teacher assigned", allocationService.assignClassTeacher(request)));
    }

    @GetMapping("/class-teachers/{assignmentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Get class teacher assignment by ID")
    public ResponseEntity<ApiResponse<ClassTeacherAssignmentResponse>> getClassTeacher(@PathVariable Long assignmentId) {
        return ResponseEntity.ok(ApiResponse.success("Assignment found", allocationService.getClassTeacherAssignment(assignmentId)));
    }

    @GetMapping("/class-teachers")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Get class teacher assignments by year and class")
    public ResponseEntity<ApiResponse<List<ClassTeacherAssignmentResponse>>> getClassTeachers(
            @RequestParam Long yearId,
            @RequestParam Long classId,
            @RequestParam(required = false) Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success("Assignments retrieved", allocationService.getClassTeacherAssignments(yearId, classId, sectionId)));
    }

    @DeleteMapping("/class-teachers/{assignmentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Remove class teacher assignment")
    public ResponseEntity<ApiResponse<Void>> removeClassTeacher(@PathVariable Long assignmentId) {
        allocationService.removeClassTeacher(assignmentId);
        return ResponseEntity.ok(ApiResponse.success("Class teacher assignment removed", null));
    }

    // ---- Subject Assignment ----

    @PostMapping("/subjects")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Assign subject to teacher")
    public ResponseEntity<ApiResponse<SubjectAssignmentResponse>> assignSubject(
            @Valid @RequestBody SubjectAssignmentRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success("Subject assigned", allocationService.assignSubject(request)));
    }

    @PutMapping("/subjects/{assignmentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Update subject assignment")
    public ResponseEntity<ApiResponse<SubjectAssignmentResponse>> updateSubject(
            @PathVariable Long assignmentId, @Valid @RequestBody SubjectAssignmentRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Subject assignment updated", allocationService.updateSubjectAssignment(assignmentId, request)));
    }

    @GetMapping("/subjects")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF','TEACHER')")
    @Operation(summary = "Get subject assignments by year and class")
    public ResponseEntity<ApiResponse<List<SubjectAssignmentResponse>>> getSubjectAssignments(
            @RequestParam Long yearId,
            @RequestParam Long classId,
            @RequestParam(required = false) Long sectionId) {
        return ResponseEntity.ok(ApiResponse.success("Assignments retrieved", allocationService.getSubjectAssignments(yearId, classId, sectionId)));
    }

    @DeleteMapping("/subjects/{assignmentId}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER')")
    @Operation(summary = "Remove subject assignment")
    public ResponseEntity<ApiResponse<Void>> removeSubject(@PathVariable Long assignmentId) {
        allocationService.removeSubjectAssignment(assignmentId);
        return ResponseEntity.ok(ApiResponse.success("Subject assignment removed", null));
    }

    // ---- Workload ----

    @GetMapping("/workload")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Operation(summary = "Get teacher workload")
    public ResponseEntity<ApiResponse<TeacherWorkloadResponse>> getWorkload(
            @RequestParam Long teacherId,
            @RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success("Teacher workload retrieved", allocationService.getTeacherWorkload(teacherId, academicYearId)));
    }
}
