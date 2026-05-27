package com.thinkerscave.common.enrollment.service;

import com.thinkerscave.common.audit.domain.AuditEventType;
import com.thinkerscave.common.audit.service.AuditPublisher;
import com.thinkerscave.common.common.sequence.SequenceGeneratorService;
import com.thinkerscave.common.context.OrganizationContext;
import com.thinkerscave.common.enrollment.domain.AcademicEnrollment;
import com.thinkerscave.common.enrollment.domain.EnrollmentStatus;
import com.thinkerscave.common.enrollment.dto.AcademicEnrollmentDTO;
import com.thinkerscave.common.enrollment.repository.AcademicEnrollmentRepository;
import com.thinkerscave.common.exception.BadRequestException;
import com.thinkerscave.common.exception.ConflictException;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Yearly academic enrollment lifecycle — create, update, transition status,
 * and look up active enrollments by year/class.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class EnrollmentService {

    private static final Set<EnrollmentStatus> TERMINAL_STATES = EnumSet.of(
            EnrollmentStatus.GRADUATED, EnrollmentStatus.TRANSFERRED_OUT,
            EnrollmentStatus.DROPPED_OUT, EnrollmentStatus.EXPELLED, EnrollmentStatus.DECEASED);

    private final AcademicEnrollmentRepository enrollmentRepository;
    private final SequenceGeneratorService sequenceGenerator;
    private final AuditPublisher auditPublisher;

    public Page<AcademicEnrollmentDTO> listByYear(Long academicYearId, Pageable pageable) {
        return enrollmentRepository
                .findByOrganizationIdAndAcademicYearId(currentOrgId(), academicYearId, pageable)
                .map(this::toDto);
    }

    public List<AcademicEnrollmentDTO> listActiveByClass(Long academicYearId, Long classId) {
        return enrollmentRepository
                .findByOrganizationIdAndAcademicYearIdAndClassIdAndStatus(
                        currentOrgId(), academicYearId, classId, EnrollmentStatus.ACTIVE)
                .stream().map(this::toDto).toList();
    }

    public AcademicEnrollmentDTO get(Long id) {
        return toDto(load(id));
    }

    @Transactional
    public AcademicEnrollmentDTO create(AcademicEnrollmentDTO dto) {
        if (dto.getStudentId() == null || dto.getAcademicYearId() == null || dto.getClassId() == null) {
            throw new BadRequestException("studentId, academicYearId and classId are required");
        }
        Long orgId = currentOrgId();
        enrollmentRepository.findByStudentIdAndAcademicYearId(dto.getStudentId(), dto.getAcademicYearId())
                .ifPresent(e -> { throw new ConflictException("Student already enrolled for this academic year"); });

        String number = dto.getEnrollmentNumber();
        if (number == null || number.isBlank()) {
            number = sequenceGenerator.nextNumber(orgId, "ENROLLMENT", null);
        } else if (enrollmentRepository.existsByEnrollmentNumberAndOrganizationId(number, orgId)) {
            throw new ConflictException("Enrollment number already exists: " + number);
        }

        AcademicEnrollment e = AcademicEnrollment.builder()
                .enrollmentNumber(number)
                .studentId(dto.getStudentId())
                .academicYearId(dto.getAcademicYearId())
                .classId(dto.getClassId())
                .sectionId(dto.getSectionId())
                .rollNumber(dto.getRollNumber())
                .house(dto.getHouse())
                .enrollmentDate(dto.getEnrollmentDate() != null ? dto.getEnrollmentDate() : LocalDate.now())
                .status(dto.getStatus() != null ? dto.getStatus() : EnrollmentStatus.ACTIVE)
                .remarks(dto.getRemarks())
                .build();
        e.setOrganizationId(orgId);
        e = enrollmentRepository.save(e);
        auditPublisher.publish(AuditEventType.CREATE, "ENROLLMENT_CREATE", "AcademicEnrollment",
                e.getId(), "Enrollment " + e.getEnrollmentNumber() + " created");
        return toDto(e);
    }

    @Transactional
    public AcademicEnrollmentDTO update(Long id, AcademicEnrollmentDTO dto) {
        AcademicEnrollment e = load(id);
        if (TERMINAL_STATES.contains(e.getStatus())) {
            throw new BadRequestException("Cannot modify a terminated enrollment");
        }
        if (dto.getClassId() != null) e.setClassId(dto.getClassId());
        if (dto.getSectionId() != null) e.setSectionId(dto.getSectionId());
        if (dto.getRollNumber() != null) e.setRollNumber(dto.getRollNumber());
        if (dto.getHouse() != null) e.setHouse(dto.getHouse());
        if (dto.getRemarks() != null) e.setRemarks(dto.getRemarks());
        e = enrollmentRepository.save(e);
        auditPublisher.publish(AuditEventType.UPDATE, "ENROLLMENT_UPDATE", "AcademicEnrollment",
                e.getId(), "Enrollment " + e.getEnrollmentNumber() + " updated");
        return toDto(e);
    }

    @Transactional
    public AcademicEnrollmentDTO transitionStatus(Long id, EnrollmentStatus target, String remarks) {
        if (target == null) throw new BadRequestException("Target status is required");
        AcademicEnrollment e = load(id);
        if (e.getStatus() == target) return toDto(e);
        if (TERMINAL_STATES.contains(e.getStatus())) {
            throw new BadRequestException("Enrollment is already in a terminal state: " + e.getStatus());
        }
        EnrollmentStatus previous = e.getStatus();
        e.setStatus(target);
        if (TERMINAL_STATES.contains(target) || target == EnrollmentStatus.PROMOTED) {
            if (e.getExitDate() == null) e.setExitDate(LocalDate.now());
        }
        if (remarks != null && !remarks.isBlank()) e.setRemarks(remarks);
        e = enrollmentRepository.save(e);
        auditPublisher.publish(AuditEventType.STATE_CHANGE, "ENROLLMENT_TRANSITION", "AcademicEnrollment",
                e.getId(), "Enrollment " + e.getEnrollmentNumber() + " transitioned " + previous + " -> " + target);
        return toDto(e);
    }

    public long activeCount(Long academicYearId) {
        return enrollmentRepository.countByOrganizationIdAndAcademicYearIdAndStatus(
                currentOrgId(), academicYearId, EnrollmentStatus.ACTIVE);
    }

    AcademicEnrollment load(Long id) {
        AcademicEnrollment e = enrollmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Enrollment not found: " + id));
        Long orgId = currentOrgId();
        if (orgId != null && !orgId.equals(e.getOrganizationId())) {
            throw new ResourceNotFoundException("Enrollment not found: " + id);
        }
        return e;
    }

    private AcademicEnrollmentDTO toDto(AcademicEnrollment e) {
        return AcademicEnrollmentDTO.builder()
                .id(e.getId())
                .enrollmentNumber(e.getEnrollmentNumber())
                .studentId(e.getStudentId())
                .academicYearId(e.getAcademicYearId())
                .classId(e.getClassId())
                .sectionId(e.getSectionId())
                .rollNumber(e.getRollNumber())
                .house(e.getHouse())
                .enrollmentDate(e.getEnrollmentDate())
                .exitDate(e.getExitDate())
                .status(e.getStatus())
                .remarks(e.getRemarks())
                .build();
    }

    private Long currentOrgId() {
        return OrganizationContext.getOrganizationId();
    }
}
