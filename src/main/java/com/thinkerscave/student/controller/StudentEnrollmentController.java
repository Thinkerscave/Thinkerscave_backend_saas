package com.thinkerscave.student.controller;

import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.dto.ApiResponse;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.student.dto.EnrollmentDTO;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.enums.EnrollmentStatus;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import com.thinkerscave.student.repository.StudentRepository;
import com.thinkerscave.student.repository.StudentTimelineRepository;
import com.thinkerscave.student.entity.StudentTimeline;
import com.thinkerscave.student.enums.StudentTimelineEventType;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/students/{studentId}/enrollment")
@Tag(name = "Student Enrollment", description = "APIs for managing student class/section enrollment")
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StudentEnrollmentController {

    private final StudentEnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final StudentTimelineRepository timelineRepository;

    @GetMapping
    @Operation(summary = "Get enrollment history for a student")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<List<EnrollmentDTO>>> getEnrollments(@PathVariable Long studentId) {
        List<EnrollmentDTO> dtos = enrollmentRepository
                .findHistoryWithDetailsByStudentId(studentId)
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success("Enrollment history retrieved", dtos));
    }

    @GetMapping("/active")
    @Operation(summary = "Get current active enrollment for a student")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    public ResponseEntity<ApiResponse<EnrollmentDTO>> getActiveEnrollment(@PathVariable Long studentId) {
        EnrollmentDTO dto = enrollmentRepository.findActiveWithClassByStudentId(studentId)
                .map(this::toDTO)
                .orElseThrow(() -> new com.thinkerscave.shared.exceptions.ResourceNotFoundException(
                        "No active enrollment found for student: " + studentId));
        return ResponseEntity.ok(ApiResponse.success("Active enrollment retrieved", dto));
    }

    @PostMapping
    @Operation(summary = "Create or update enrollment for a student (change class/section)")
    @PreAuthorize("hasAnyAuthority('SUPER_ADMIN','ORGANIZATION_ADMIN','ORGANIZATION_OWNER','STAFF')")
    @Transactional
    public ResponseEntity<ApiResponse<EnrollmentDTO>> updateEnrollment(
            @PathVariable Long studentId,
            @Valid @RequestBody EnrollmentUpdateRequest request) {

        var student = studentRepository.findById(studentId)
            .orElseThrow(() -> new com.thinkerscave.shared.exceptions.ResourceNotFoundException(
                "Student not found: " + studentId));
        var academicYear = academicYearRepository.findById(request.getAcademicYearId())
            .orElseThrow(() -> new com.thinkerscave.shared.exceptions.ResourceNotFoundException(
                "Academic year not found: " + request.getAcademicYearId()));
        var classEntity = classRepository.findById(request.getClassId())
            .orElseThrow(() -> new com.thinkerscave.shared.exceptions.ResourceNotFoundException(
                "Class not found: " + request.getClassId()));
        if (classEntity.getAcademicYear() == null
            || !classEntity.getAcademicYear().getAcademicYearId().equals(academicYear.getAcademicYearId())) {
            throw new BadRequestException("Class does not belong to the selected academic year");
        }
        if (!classEntity.isActive()) {
            throw new BadRequestException("Cannot enroll a student in an inactive class");
        }
        var section = request.getSectionId() == null ? null : sectionRepository.findById(request.getSectionId())
            .orElseThrow(() -> new com.thinkerscave.shared.exceptions.ResourceNotFoundException(
                "Section not found: " + request.getSectionId()));
        if (section != null && (section.getAcademicGroup() == null
            || !section.getAcademicGroup().getAcademicGroupId().equals(classEntity.getClassId()))) {
            throw new BadRequestException("Section does not belong to the selected class");
        }

        var activeEnrollment = enrollmentRepository.findByStudentStudentIdAndActiveTrue(studentId);
        if (activeEnrollment.isPresent()
            && activeEnrollment.get().getAcademicYear() != null
            && activeEnrollment.get().getAcademicYear().getAcademicYearId().equals(academicYear.getAcademicYearId())) {
            StudentEnrollment current = activeEnrollment.get();
            boolean placementChanged = !java.util.Objects.equals(
                current.getClassEntity() == null ? null : current.getClassEntity().getClassId(), classEntity.getClassId())
                || !java.util.Objects.equals(
                current.getSection() == null ? null : current.getSection().getSectionId(),
                section == null ? null : section.getSectionId());
            if (!placementChanged) {
            current.setRollNumber(request.getRollNumber());
            current.setRemarks(request.getRemarks());
            return ResponseEntity.ok(ApiResponse.success("Enrollment updated", toDTO(enrollmentRepository.save(current))));
            }
            current.setActive(false);
            current.setStatus(EnrollmentStatus.REASSIGNED);
            enrollmentRepository.save(current);
            StudentEnrollment reassigned = new StudentEnrollment();
            reassigned.setStudent(student);
            reassigned.setAcademicYear(academicYear);
            reassigned.setClassEntity(classEntity);
            reassigned.setSection(section);
            reassigned.setRollNumber(request.getRollNumber());
            reassigned.setRemarks(request.getRemarks());
            reassigned.setStatus(EnrollmentStatus.ACTIVE);
            reassigned.setActive(true);
            StudentEnrollment saved = enrollmentRepository.save(reassigned);
            StudentTimeline timeline = new StudentTimeline();
            timeline.setStudent(student);
            timeline.setEventType(StudentTimelineEventType.CLASS_CHANGED);
            timeline.setTitle("Student enrollment reassigned");
            timeline.setDescription("Student placement changed from " + enrollmentLocation(current)
                + " to " + enrollmentLocation(saved) + " for academic year " + academicYear.getYearCode());
            timelineRepository.save(timeline);
            return ResponseEntity.ok(ApiResponse.success("Enrollment reassigned", toDTO(saved)));
        }
        if (enrollmentRepository.existsByStudentStudentIdAndAcademicYearAcademicYearId(
            studentId, academicYear.getAcademicYearId())) {
            throw new BadRequestException("Student already has enrollment history for this academic year");
        }

        activeEnrollment.ifPresent(e -> {
            e.setActive(false);
            enrollmentRepository.save(e);
        });

        StudentEnrollment enrollment = new StudentEnrollment();
        enrollment.setStudent(student);
        enrollment.setRollNumber(request.getRollNumber());
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setActive(true);
        enrollment.setRemarks(request.getRemarks());
        enrollment.setAcademicYear(academicYear);
        enrollment.setClassEntity(classEntity);
        enrollment.setSection(section);

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
        if (e.getAcademicYear() != null) {
            dto.setAcademicYearId(e.getAcademicYear().getAcademicYearId());
            dto.setAcademicYear(e.getAcademicYear().getYearCode());
        }
        if (e.getClassEntity() != null) {
            dto.setClassId(e.getClassEntity().getClassId());
            dto.setClassName(e.getClassEntity().getClassName());
        }
        if (e.getSection() != null) {
            dto.setSectionId(e.getSection().getSectionId());
            dto.setSectionName(e.getSection().getSectionName());
        }
        if (e.getCreatedOn() != null) {
            dto.setEnrollmentDate(e.getCreatedOn().toLocalDate().toString());
        }
        return dto;
    }

    private String enrollmentLocation(StudentEnrollment enrollment) {
        String className = enrollment.getClassEntity() == null ? "Unassigned class" : enrollment.getClassEntity().getClassName();
        String sectionName = enrollment.getSection() == null ? "Unassigned section" : enrollment.getSection().getSectionName();
        return className + " / " + sectionName;
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
