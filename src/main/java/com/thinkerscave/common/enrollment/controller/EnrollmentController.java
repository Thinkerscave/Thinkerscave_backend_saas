package com.thinkerscave.common.enrollment.controller;

import com.thinkerscave.common.common.util.PageRequestUtil;
import com.thinkerscave.common.dto.ApiResponse;
import com.thinkerscave.common.dto.PageResponse;
import com.thinkerscave.common.enrollment.domain.EnrollmentStatus;
import com.thinkerscave.common.enrollment.dto.AcademicEnrollmentDTO;
import com.thinkerscave.common.enrollment.service.EnrollmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/enrollments")
@Tag(name = "Enrollments", description = "Per-year academic enrollment lifecycle")
@RequiredArgsConstructor
@Slf4j
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('ENROLLMENT_VIEW')")
    @Operation(summary = "Paginated enrollments by academic year")
    public ResponseEntity<ApiResponse<PageResponse<AcademicEnrollmentDTO>>> list(
            @RequestParam Long academicYearId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(ApiResponse.success(PageResponse.of(
                enrollmentService.listByYear(academicYearId, PageRequestUtil.of(page, size, sort)))));
    }

    @GetMapping("/active")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF','TEACHER') or hasAuthority('ENROLLMENT_VIEW')")
    @Operation(summary = "Active enrollments for a class within an academic year")
    public ResponseEntity<ApiResponse<List<AcademicEnrollmentDTO>>> active(
            @RequestParam Long academicYearId,
            @RequestParam Long classId) {
        return ResponseEntity.ok(ApiResponse.success(
                enrollmentService.listActiveByClass(academicYearId, classId)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF','TEACHER') or hasAuthority('ENROLLMENT_VIEW')")
    public ResponseEntity<ApiResponse<AcademicEnrollmentDTO>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.get(id)));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('ENROLLMENT_EDIT')")
    public ResponseEntity<ApiResponse<AcademicEnrollmentDTO>> create(
            @Valid @RequestBody AcademicEnrollmentDTO dto) {
        return ResponseEntity.ok(ApiResponse.created("Enrollment created", enrollmentService.create(dto)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('ENROLLMENT_EDIT')")
    public ResponseEntity<ApiResponse<AcademicEnrollmentDTO>> update(
            @PathVariable Long id, @Valid @RequestBody AcademicEnrollmentDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Enrollment updated", enrollmentService.update(id, dto)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN') or hasAuthority('ENROLLMENT_EDIT')")
    @Operation(summary = "Transition enrollment status")
    public ResponseEntity<ApiResponse<AcademicEnrollmentDTO>> transition(
            @PathVariable Long id,
            @RequestParam EnrollmentStatus target,
            @RequestParam(required = false) String remarks) {
        return ResponseEntity.ok(ApiResponse.success("Status updated",
                enrollmentService.transitionStatus(id, target, remarks)));
    }

    @GetMapping("/count/active")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ADMIN','STAFF') or hasAuthority('ENROLLMENT_VIEW')")
    public ResponseEntity<ApiResponse<Long>> activeCount(@RequestParam Long academicYearId) {
        return ResponseEntity.ok(ApiResponse.success(enrollmentService.activeCount(academicYearId)));
    }
}
