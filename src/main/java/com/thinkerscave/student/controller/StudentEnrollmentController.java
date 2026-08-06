package com.thinkerscave.student.controller;

import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.student.dto.EnrollmentDTO;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.enums.EnrollmentStatus;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/students/{studentId}/enrollment")
@Tag(name = "Student Enrollment", description = "APIs for managing student class/section enrollment")
@RequiredArgsConstructor
@Slf4j
public class StudentEnrollmentController {

    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;

    @GetMapping
    @Operation(summary = "Get enrollment history for a student")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<List<EnrollmentDTO>>> getEnrollments(@PathVariable Long studentId) {
        List<EnrollmentDTO> dtos = enrollmentRepository
                .findByStudentStudentIdOrderByEnrollmentIdDesc(studentId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Enrollment history retrieved", dtos));
    }

    @GetMapping("/active")
    @Operation(summary = "Get current active enrollment for a student")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<EnrollmentDTO>> getActiveEnrollment(@PathVariable Long studentId) {
        EnrollmentDTO dto = enrollmentRepository.findByStudentStudentIdAndActiveTrue(studentId)
                .map(this::toDTO)
                .orElseThrow(() -> new com.thinkerscave.shared.exceptions.ResourceNotFoundException(
                        "No active enrollment found for student: " + studentId));
        return ResponseEntity.ok(ApiResponse.success("Active enrollment retrieved", dto));
    }

    @PostMapping
    @Operation(summary = "Create or update enrollment for a student (change class/section)")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<EnrollmentDTO>> updateEnrollment(
            @PathVariable Long studentId,
            @Valid @RequestBody EnrollmentUpdateRequest request) {

        // Deactivate existing active enrollment
        enrollmentRepository.findByStudentStudentIdAndActiveTrue(studentId).ifPresent(e -> {
            e.setActive(false);
            enrollmentRepository.save(e);
        });

        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudent(studentRepository.findById(studentId)
                .orElseThrow(() -> new com.thinkerscave.shared.exceptions.ResourceNotFoundException("Student not found: " + studentId)));
        enrollment.setRollNumber(request.getRollNumber());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setActive(true);
        enrollment.setRemarks(request.getRemarks());

        academicYearRepository.findById(request.getAcademicYearId())
                .ifPresent(enrollment::setAcademicYear);
        classRepository.findById(request.getClassId())
                .ifPresent(enrollment::setClassEntity);
        if (request.getSectionId() != null) {
            sectionRepository.findById(request.getSectionId()).ifPresent(enrollment::setSection);
        }

        StudentEnrollment saved = enrollmentRepository.save(enrollment);
        return ResponseEntity.ok(ApiResponse.success("Enrollment updated", toDTO(saved)));
    }

    @GetMapping("/class/{classId}/students")
    @Operation(summary = "Get all active students in a class/section")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<List<EnrollmentDTO>>> getStudentsByClass(
            @PathVariable Long studentId,
            @PathVariable Long classId,
            @RequestParam(required = false) Long sectionId) {
        List<EnrollmentDTO> dtos;
        if (sectionId != null) {
            dtos = enrollmentRepository
                    .findByClassEntityClassIdAndSectionSectionIdAndActiveTrueOrderByRollNumber(classId, sectionId)
                    .stream().map(this::toDTO).collect(Collectors.toList());
        } else {
            dtos = enrollmentRepository
                    .findByClassEntityClassIdAndActiveTrueOrderByStudentFirstNameAsc(classId)
                    .stream().map(this::toDTO).collect(Collectors.toList());
        }
        return ResponseEntity.ok(ApiResponse.success("Students retrieved", dtos));
    }

    private EnrollmentDTO toDTO(StudentEnrollment e) {
        EnrollmentDTO dto = new EnrollmentDTO();
        dto.setEnrollmentId(e.getEnrollmentId());
        dto.setRollNumber(e.getRollNumber());
        dto.setStatus(e.getStatus() != null ? e.getStatus().name() : null);
        if (e.getAcademicYear() != null) dto.setAcademicYear(e.getAcademicYear().getYearCode());
        if (e.getClassEntity() != null) dto.setClassName(e.getClassEntity().getClassName());
        if (e.getSection() != null) dto.setSectionName(e.getSection().getSectionName());
        return dto;
    }

    @Data
    public static class EnrollmentUpdateRequest {
        @NotNull
        private Long academicYearId;
        @NotNull
        private Long classId;
        private Long sectionId;
        private String rollNumber;
        private String remarks;
    }
}
