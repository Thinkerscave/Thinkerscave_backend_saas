package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.AcademicYearRequest;
import com.thinkerscave.academics.dto.request.RejectAcademicYearRequest;
import com.thinkerscave.academics.dto.response.AcademicYearDashboardResponse;
import com.thinkerscave.academics.dto.response.AcademicYearResponse;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.ClassSubjectMappingRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.repository.SubjectRepository;
import com.thinkerscave.academics.repository.TeacherAllocationRepository;
import com.thinkerscave.academics.repository.TimetableConfigurationRepository;
import com.thinkerscave.academics.repository.TimetableVersionRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.AcademicYearService;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class AcademicYearServiceImpl implements AcademicYearService {

    private static final Set<AcademicYearStatus> UPCOMING_STATUSES = EnumSet.of(
            AcademicYearStatus.DRAFT,
            AcademicYearStatus.PREPARING,
            AcademicYearStatus.READY_FOR_APPROVAL,
            AcademicYearStatus.PENDING_APPROVAL,
            AcademicYearStatus.APPROVED,
            AcademicYearStatus.REJECTED
    );

    private static final Set<AcademicYearStatus> EDITABLE_STATUSES = EnumSet.of(
            AcademicYearStatus.DRAFT,
            AcademicYearStatus.PREPARING,
            AcademicYearStatus.READY_FOR_APPROVAL,
            AcademicYearStatus.REJECTED
    );

    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final SubjectRepository subjectRepository;
    private final ClassSubjectMappingRepository classSubjectMappingRepository;
    private final TeacherAllocationRepository teacherAllocationRepository;
    private final TimetableConfigurationRepository timetableConfigurationRepository;
    private final TimetableVersionRepository timetableVersionRepository;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final AcademicsAccessGuard accessGuard;

    @Override
    @Transactional(readOnly = true)
    public AcademicYearDashboardResponse getDashboard() {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_ACADEMIC_YEAR);

        AcademicYear current = academicYearRepository.findByStatusAndActiveTrue(AcademicYearStatus.CURRENT).orElse(null);
        AcademicYear upcoming = academicYearRepository.findByActiveTrueOrderByStartDateDesc().stream()
                .filter(y -> UPCOMING_STATUSES.contains(y.getStatus()))
                .max(Comparator.comparing(AcademicYear::getStartDate))
                .orElse(null);

        List<AcademicYearResponse> history = academicYearRepository.findAll(
                        org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "startDate"))
                .stream()
                .map(y -> toResponse(y, true))
                .toList();

        return AcademicYearDashboardResponse.builder()
                .currentYear(current == null ? null : toResponse(current, true))
                .upcomingYear(upcoming == null ? null : toResponse(upcoming, true))
                .history(history)
                .totalYears(history.size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AcademicYearResponse> search(String query, AcademicYearStatus status, Pageable pageable) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_ACADEMIC_YEAR);

        Page<AcademicYear> page;
        boolean hasQuery = StringUtils.hasText(query);
        if (hasQuery && status != null) {
            page = academicYearRepository.findByNameContainingIgnoreCaseAndStatusOrderByStartDateDesc(
                    query.trim(), status, pageable);
        } else if (hasQuery) {
            page = academicYearRepository.findByNameContainingIgnoreCase(query.trim(), pageable);
        } else if (status != null) {
            page = academicYearRepository.findByStatusOrderByStartDateDesc(status, pageable);
        } else {
            page = academicYearRepository.findAllByOrderByStartDateDesc(pageable);
        }
        return page.map(y -> toResponse(y, false));
    }

    @Override
    @Transactional(readOnly = true)
    public AcademicYearResponse getById(Long id) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_ACADEMIC_YEAR);
        return toResponse(requireYear(id), true);
    }

    @Override
    public AcademicYearResponse create(AcademicYearRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_ACADEMIC_YEAR);
        validateDates(request.getStartDate(), request.getEndDate());

        if (academicYearRepository.existsByNameIgnoreCase(request.getName().trim())) {
            throw new BusinessException("Academic year name already exists: " + request.getName());
        }
        if (academicYearRepository.existsOverlappingActiveRange(
                request.getStartDate(), request.getEndDate(), null)) {
            throw new BusinessException("Academic year dates overlap an existing active academic year");
        }

        AcademicYear year = new AcademicYear();
        year.setName(request.getName().trim());
        year.setStartDate(request.getStartDate());
        year.setEndDate(request.getEndDate());
        year.setPattern(request.getPattern());
        year.setStatus(AcademicYearStatus.PREPARING);
        year.setActive(true);
        return toResponse(academicYearRepository.save(year), true);
    }

    @Override
    public AcademicYearResponse update(Long id, AcademicYearRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_ACADEMIC_YEAR);
        AcademicYear year = requireYear(id);
        assertEditable(year);
        validateDates(request.getStartDate(), request.getEndDate());

        if (academicYearRepository.existsByNameIgnoreCaseAndAcademicYearIdNot(request.getName().trim(), id)) {
            throw new BusinessException("Academic year name already exists: " + request.getName());
        }
        if (academicYearRepository.existsOverlappingActiveRange(
                request.getStartDate(), request.getEndDate(), id)) {
            throw new BusinessException("Academic year dates overlap an existing active academic year");
        }

        year.setName(request.getName().trim());
        year.setStartDate(request.getStartDate());
        year.setEndDate(request.getEndDate());
        year.setPattern(request.getPattern());
        return toResponse(academicYearRepository.save(year), true);
    }

    @Override
    public AcademicYearResponse deactivate(Long id) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_ACADEMIC_YEAR);
        AcademicYear year = requireYear(id);
        if (year.getStatus() == AcademicYearStatus.CURRENT) {
            throw new BusinessException("Cannot deactivate the current academic year. Activate another year first.");
        }
        year.setActive(false);
        return toResponse(academicYearRepository.save(year), false);
    }

    @Override
    public AcademicYearResponse markReadyForApproval(Long id) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_ACADEMIC_YEAR);
        AcademicYear year = requireYear(id);
        if (year.getStatus() != AcademicYearStatus.PREPARING
                && year.getStatus() != AcademicYearStatus.DRAFT
                && year.getStatus() != AcademicYearStatus.REJECTED) {
            throw new BusinessException("Only preparing or rejected years can be marked ready for approval");
        }
        year.setStatus(AcademicYearStatus.READY_FOR_APPROVAL);
        year.setRejectionReason(null);
        return toResponse(academicYearRepository.save(year), true);
    }

    @Override
    public AcademicYearResponse submitForApproval(Long id) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_ACADEMIC_YEAR);
        AcademicYear year = requireYear(id);
        if (year.getStatus() != AcademicYearStatus.READY_FOR_APPROVAL
                && year.getStatus() != AcademicYearStatus.PREPARING
                && year.getStatus() != AcademicYearStatus.REJECTED) {
            throw new BusinessException("Academic year is not ready for submission");
        }
        year.setStatus(AcademicYearStatus.PENDING_APPROVAL);
        year.setSubmittedAt(LocalDateTime.now());
        year.setSubmittedByUserId(accessGuard.currentUserIdOrNull());
        year.setRejectionReason(null);
        return toResponse(academicYearRepository.save(year), true);
    }

    @Override
    public AcademicYearResponse approve(Long id) {
        accessGuard.requireApprove(AcademicsAccessGuard.RESOURCE_ACADEMIC_YEAR);
        AcademicYear year = requireYear(id);
        if (year.getStatus() != AcademicYearStatus.PENDING_APPROVAL
                && year.getStatus() != AcademicYearStatus.READY_FOR_APPROVAL) {
            throw new BusinessException("Only pending or ready academic years can be approved");
        }
        year.setStatus(AcademicYearStatus.APPROVED);
        year.setApprovedAt(LocalDateTime.now());
        year.setApprovedByUserId(accessGuard.currentUserIdOrNull());
        year.setRejectedAt(null);
        year.setRejectedByUserId(null);
        year.setRejectionReason(null);
        return toResponse(academicYearRepository.save(year), true);
    }

    @Override
    public AcademicYearResponse reject(Long id, RejectAcademicYearRequest request) {
        accessGuard.requireApprove(AcademicsAccessGuard.RESOURCE_ACADEMIC_YEAR);
        AcademicYear year = requireYear(id);
        if (year.getStatus() != AcademicYearStatus.PENDING_APPROVAL
                && year.getStatus() != AcademicYearStatus.READY_FOR_APPROVAL) {
            throw new BusinessException("Only pending or ready academic years can be rejected");
        }
        year.setStatus(AcademicYearStatus.REJECTED);
        year.setRejectedAt(LocalDateTime.now());
        year.setRejectedByUserId(accessGuard.currentUserIdOrNull());
        year.setRejectionReason(request.getRejectionReason().trim());
        return toResponse(academicYearRepository.save(year), true);
    }

    @Override
    public AcademicYearResponse activate(Long id) {
        accessGuard.requireApprove(AcademicsAccessGuard.RESOURCE_ACADEMIC_YEAR);
        AcademicYear year = requireYear(id);
        if (year.getStatus() != AcademicYearStatus.APPROVED) {
            throw new BusinessException("Only an approved academic year can be activated");
        }
        if (!Boolean.TRUE.equals(year.getActive())) {
            throw new BusinessException("Cannot activate an inactive academic year");
        }

        academicYearRepository.clearCurrentYearStatus();
        year.setStatus(AcademicYearStatus.CURRENT);
        year.setActivatedAt(LocalDateTime.now());
        year.setActivatedByUserId(accessGuard.currentUserIdOrNull());
        return toResponse(academicYearRepository.save(year), true);
    }

    private AcademicYear requireYear(Long id) {
        return academicYearRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + id));
    }

    private void assertEditable(AcademicYear year) {
        if (!EDITABLE_STATUSES.contains(year.getStatus()) || !Boolean.TRUE.equals(year.getActive())) {
            throw new BusinessException("Academic year is read-only in status " + year.getStatus());
        }
    }

    private void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null) {
            throw new BusinessException("Start date and end date are mandatory");
        }
        if (!end.isAfter(start)) {
            throw new BusinessException("End date must be after start date");
        }
    }

    private AcademicYearResponse toResponse(AcademicYear year, boolean includeDetails) {
        AcademicYearResponse.AcademicYearResponseBuilder builder = AcademicYearResponse.builder()
                .academicYearId(year.getAcademicYearId())
                .name(year.getName())
                .startDate(year.getStartDate())
                .endDate(year.getEndDate())
                .pattern(year.getPattern())
                .status(year.getStatus())
                .active(year.getActive())
                .submittedAt(year.getSubmittedAt())
                .submittedByUserId(year.getSubmittedByUserId())
                .approvedAt(year.getApprovedAt())
                .approvedByUserId(year.getApprovedByUserId())
                .rejectedAt(year.getRejectedAt())
                .rejectedByUserId(year.getRejectedByUserId())
                .rejectionReason(year.getRejectionReason())
                .activatedAt(year.getActivatedAt())
                .activatedByUserId(year.getActivatedByUserId())
                .createdBy(year.getCreatedBy())
                .createdOn(year.getCreatedOn())
                .updatedBy(year.getUpdatedBy())
                .updatedOn(year.getUpdatedOn());

        applyProgress(builder, year);

        if (includeDetails) {
            builder.structureStats(buildStructureStats(year.getAcademicYearId()));
            List<AcademicYearResponse.ReadinessStep> steps = buildReadinessSteps(year.getAcademicYearId());
            long completed = steps.stream().filter(s -> "COMPLETE".equals(s.getState())).count();
            builder.readinessSteps(steps)
                    .readinessCompletedSteps((int) completed)
                    .readinessTotalSteps(steps.size())
                    .readinessPercent(steps.isEmpty() ? 0 : (int) Math.round((completed * 100.0) / steps.size()));
        }
        return builder.build();
    }

    private void applyProgress(AcademicYearResponse.AcademicYearResponseBuilder builder, AcademicYear year) {
        LocalDate today = LocalDate.now();
        long totalDays = ChronoUnit.DAYS.between(year.getStartDate(), year.getEndDate()) + 1;
        if (totalDays <= 0) {
            builder.progressPercent(0).daysCompleted(0L).daysRemaining(0L);
            return;
        }
        long completed;
        if (today.isBefore(year.getStartDate())) {
            completed = 0;
        } else if (today.isAfter(year.getEndDate())) {
            completed = totalDays;
        } else {
            completed = ChronoUnit.DAYS.between(year.getStartDate(), today) + 1;
        }
        long remaining = Math.max(0, totalDays - completed);
        int percent = (int) Math.round((completed * 100.0) / totalDays);
        builder.progressPercent(percent).daysCompleted(completed).daysRemaining(remaining);
    }

    private AcademicYearResponse.YearStructureStats buildStructureStats(Long yearId) {
        var classes = classRepository.findByAcademicYear_AcademicYearIdOrderByDisplayOrderAsc(yearId);
        long classesActive = classes.stream().filter(c -> Boolean.TRUE.equals(c.getActive())).count();

        long sectionsTotal = 0;
        long sectionsActive = 0;
        for (var cls : classes) {
            var sections = sectionRepository.findByAcademicClass_ClassIdOrderByDisplayOrderAsc(cls.getClassId());
            sectionsTotal += sections.size();
            sectionsActive += sections.stream().filter(s -> Boolean.TRUE.equals(s.getActive())).count();
        }

        var subjects = subjectRepository.findByAcademicYear_AcademicYearIdOrderByNameAsc(yearId);
        long subjectsActive = subjects.stream().filter(s -> Boolean.TRUE.equals(s.getActive())).count();
        long studentsActive = studentEnrollmentRepository.countByAcademicYearAcademicYearIdAndActiveTrue(yearId);

        return AcademicYearResponse.YearStructureStats.builder()
                .classesTotal(classes.size())
                .classesActive(classesActive)
                .sectionsTotal(sectionsTotal)
                .sectionsActive(sectionsActive)
                .subjectsTotal(subjects.size())
                .subjectsActive(subjectsActive)
                .studentsActive(studentsActive)
                .build();
    }

    private List<AcademicYearResponse.ReadinessStep> buildReadinessSteps(Long yearId) {
        List<AcademicYearResponse.ReadinessStep> steps = new ArrayList<>();

        steps.add(step("YEAR_CREATED", "Academic Year Created", "COMPLETE", null));

        var classes = classRepository.findByAcademicYear_AcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(yearId);
        boolean hasClasses = !classes.isEmpty();
        boolean hasSections = classes.stream()
                .anyMatch(c -> !sectionRepository.findByAcademicClass_ClassIdAndActiveTrueOrderByDisplayOrderAsc(c.getClassId()).isEmpty());
        steps.add(step("CLASSES_SECTIONS", "Classes & Sections",
                hasClasses && hasSections ? "COMPLETE" : (hasClasses ? "IN_PROGRESS" : "NOT_STARTED"), null));

        var subjects = subjectRepository.findByAcademicYear_AcademicYearIdAndActiveTrueOrderByNameAsc(yearId);
        steps.add(step("SUBJECTS", "Subjects",
                subjects.isEmpty() ? "NOT_STARTED" : "COMPLETE", null));

        long mappingCount = classes.stream()
                .mapToLong(c -> classSubjectMappingRepository.findByAcademicClass_ClassIdAndActiveTrue(c.getClassId()).size())
                .sum();
        steps.add(step("SUBJECT_MAPPING", "Subject Mapping",
                mappingCount == 0 ? (subjects.isEmpty() ? "NOT_STARTED" : "PENDING") : "COMPLETE",
                mappingCount == 0 && !subjects.isEmpty() ? "Mappings pending" : null));

        long allocationCount = teacherAllocationRepository
                .findBySection_AcademicClass_AcademicYear_AcademicYearId(yearId).stream()
                .filter(a -> Boolean.TRUE.equals(a.getActive()))
                .count();
        steps.add(step("TEACHER_ALLOCATION", "Teacher Allocation",
                allocationCount == 0 ? "PENDING" : "COMPLETE",
                allocationCount == 0 ? "Allocations pending" : null));

        boolean hasConfig = !timetableConfigurationRepository.findByAcademicYear_AcademicYearId(yearId).isEmpty();
        steps.add(step("TIMETABLE_CONFIG", "Timetable Configuration",
                hasConfig ? "IN_PROGRESS" : "NOT_STARTED", null));

        boolean hasVersion = !timetableVersionRepository
                .findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(yearId).isEmpty();
        steps.add(step("TIMETABLE_GENERATION", "Timetable Generation",
                hasVersion ? "COMPLETE" : "NOT_STARTED", null));

        steps.add(step("STUDENT_PROMOTION", "Student Promotion Plan", "NOT_STARTED", null));
        steps.add(step("FINAL_APPROVAL", "Final Approval", "NOT_STARTED", null));
        steps.add(step("ACTIVATION", "Activation", "NOT_STARTED", null));
        return steps;
    }

    private AcademicYearResponse.ReadinessStep step(String code, String label, String state, String detail) {
        return AcademicYearResponse.ReadinessStep.builder()
                .code(code)
                .label(label)
                .state(state)
                .detail(detail)
                .build();
    }
}
