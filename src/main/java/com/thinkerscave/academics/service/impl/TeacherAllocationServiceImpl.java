package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.request.ClassTeacherAssignmentRequest;
import com.thinkerscave.academics.dto.request.TeacherAllocationAssignRequest;
import com.thinkerscave.academics.dto.response.ClassTeacherAssignmentResponse;
import com.thinkerscave.academics.dto.response.TeacherAllocationDashboardResponse;
import com.thinkerscave.academics.dto.response.TeacherAllocationRowResponse;
import com.thinkerscave.academics.dto.response.TeacherRecommendationResponse;
import com.thinkerscave.academics.dto.response.TeacherWorkloadResponse;
import com.thinkerscave.academics.entity.AcademicClass;
import com.thinkerscave.academics.entity.AcademicSection;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.ClassSubjectMapping;
import com.thinkerscave.academics.entity.ClassTeacherAssignment;
import com.thinkerscave.academics.entity.TeacherAllocation;
import com.thinkerscave.academics.entity.TeacherAllocationTeacher;
import com.thinkerscave.academics.entity.TimetableConfiguration;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.enums.TeacherAllocationStatus;
import com.thinkerscave.academics.enums.TeacherAllocationTeacherRole;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.ClassRepository;
import com.thinkerscave.academics.repository.ClassSubjectMappingRepository;
import com.thinkerscave.academics.repository.ClassTeacherAssignmentRepository;
import com.thinkerscave.academics.repository.SectionRepository;
import com.thinkerscave.academics.repository.TeacherAllocationRepository;
import com.thinkerscave.academics.repository.TeacherAllocationTeacherRepository;
import com.thinkerscave.academics.repository.TimetableConfigurationRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.TeacherAllocationService;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.StaffType;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class TeacherAllocationServiceImpl implements TeacherAllocationService {

    private static final int FALLBACK_MAX_WEEKLY_PERIODS = 24;
    private static final Set<AcademicYearStatus> READ_ONLY_YEAR_STATUSES = EnumSet.of(
            AcademicYearStatus.COMPLETED,
            AcademicYearStatus.ARCHIVED
    );

    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final ClassSubjectMappingRepository mappingRepository;
    private final TeacherAllocationRepository allocationRepository;
    private final TeacherAllocationTeacherRepository allocationTeacherRepository;
    private final ClassTeacherAssignmentRepository classTeacherAssignmentRepository;
    private final TimetableConfigurationRepository timetableConfigurationRepository;
    private final StaffRepository staffRepository;
    private final AcademicsAccessGuard accessGuard;

    @Override
    @Transactional(readOnly = true)
    public TeacherAllocationDashboardResponse getDashboard(
            Long academicYearId,
            Long classId,
            Long sectionId,
            Long subjectId,
            TeacherAllocationStatus status) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TEACHER_ALLOCATION);
        AcademicYear year = requireYear(academicYearId);
        int maxPeriods = resolveMaxWeeklyPeriods(academicYearId);
        boolean fromConfig = hasConfiguredMax(academicYearId);

        Map<Long, Integer> workloadByStaff = computeWorkloadMap(academicYearId);
        List<TeacherAllocationRowResponse> rows = buildRows(year, maxPeriods, workloadByStaff).stream()
                .filter(r -> classId == null || Objects.equals(r.getClassId(), classId))
                .filter(r -> sectionId == null || Objects.equals(r.getSectionId(), sectionId))
                .filter(r -> subjectId == null || Objects.equals(r.getSubjectId(), subjectId))
                .filter(r -> status == null || r.getStatus() == status)
                .toList();

        long assigned = rows.stream().filter(r -> r.getStatus() == TeacherAllocationStatus.ASSIGNED).count();
        long missing = rows.stream().filter(r -> r.getStatus() == TeacherAllocationStatus.UNASSIGNED).count();
        long conflict = rows.stream().filter(r -> r.getStatus() == TeacherAllocationStatus.CONFLICT).count();

        return TeacherAllocationDashboardResponse.builder()
                .academicYearId(year.getAcademicYearId())
                .academicYearName(year.getName())
                .academicYearStatus(year.getStatus())
                .yearReadOnly(isYearReadOnly(year))
                .maxWeeklyPeriods(maxPeriods)
                .maxWeeklyPeriodsFromConfig(fromConfig)
                .totalSlots(rows.size())
                .assignedSlots(assigned)
                .missingSlots(missing)
                .conflictSlots(conflict)
                .rows(rows)
                .workloads(listWorkloadsInternal(academicYearId, maxPeriods, workloadByStaff))
                .build();
    }

    @Override
    public TeacherAllocationRowResponse assign(TeacherAllocationAssignRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TEACHER_ALLOCATION);

        AcademicSection section = sectionRepository.findByIdWithClass(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + request.getSectionId()));
        assertYearMutable(section.getAcademicClass().getAcademicYear());

        ClassSubjectMapping mapping = mappingRepository.findById(request.getClassSubjectMappingId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class subject mapping not found: " + request.getClassSubjectMappingId()));
        if (!mapping.isActive()) {
            throw new BusinessException("Cannot assign teacher to an inactive subject mapping");
        }
        if (!Objects.equals(mapping.getAcademicClass().getClassId(), section.getAcademicClass().getClassId())) {
            throw new BusinessException("Subject mapping does not belong to this section's class");
        }

        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + request.getStaffId()));

        TeacherAllocation allocation = allocationRepository
                .findBySection_SectionIdAndClassSubjectMapping_ClassSubjectMappingId(
                        section.getSectionId(), mapping.getClassSubjectMappingId())
                .orElseGet(TeacherAllocation::new);

        if (allocation.getTeacherAllocationId() == null) {
            allocation.setSection(section);
            allocation.setClassSubjectMapping(mapping);
        }
        allocation.setActive(true);

        TeacherAllocationTeacherRole role = request.getRole() == null
                ? TeacherAllocationTeacherRole.PRIMARY
                : request.getRole();

        Long yearId = section.getAcademicClass().getAcademicYear().getAcademicYearId();
        int maxPeriods = resolveMaxWeeklyPeriods(yearId);

        closeOpenRole(allocation, role);

        allocation.setStatus(TeacherAllocationStatus.ASSIGNED);
        TeacherAllocation saved = allocationRepository.save(allocation);

        TeacherAllocationTeacher assignment = new TeacherAllocationTeacher();
        assignment.setTeacherAllocation(saved);
        assignment.setStaff(staff);
        assignment.setRole(role);
        assignment.setEffectiveFrom(LocalDate.now());
        assignment.setActive(true);
        allocationTeacherRepository.save(assignment);

        Map<Long, Integer> workload = computeWorkloadMap(yearId);
        if (workload.getOrDefault(staff.getStaffId(), 0) > maxPeriods) {
            saved.setStatus(TeacherAllocationStatus.CONFLICT);
            saved = allocationRepository.save(saved);
        }
        return toRow(saved, maxPeriods, workload);
    }

    @Override
    public TeacherAllocationRowResponse unassign(Long teacherAllocationId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TEACHER_ALLOCATION);
        TeacherAllocation allocation = allocationRepository.findByIdWithDetails(teacherAllocationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Teacher allocation not found: " + teacherAllocationId));
        assertYearMutable(allocation.getSection().getAcademicClass().getAcademicYear());

        closeOpenRole(allocation, TeacherAllocationTeacherRole.PRIMARY);
        closeOpenRole(allocation, TeacherAllocationTeacherRole.SECONDARY);
        allocation.setStatus(TeacherAllocationStatus.UNASSIGNED);
        TeacherAllocation saved = allocationRepository.save(allocation);

        Long yearId = saved.getSection().getAcademicClass().getAcademicYear().getAcademicYearId();
        int maxPeriods = resolveMaxWeeklyPeriods(yearId);
        return toRow(saved, maxPeriods, computeWorkloadMap(yearId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherRecommendationResponse> recommendations(Long sectionId, Long classSubjectMappingId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TEACHER_ALLOCATION);
        AcademicSection section = sectionRepository.findByIdWithClass(sectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));
        ClassSubjectMapping mapping = mappingRepository.findById(classSubjectMappingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Class subject mapping not found: " + classSubjectMappingId));

        Long yearId = section.getAcademicClass().getAcademicYear().getAcademicYearId();
        int maxPeriods = resolveMaxWeeklyPeriods(yearId);
        short weekly = mapping.getWeeklyPeriods() == null ? 0 : mapping.getWeeklyPeriods();
        Map<Long, Integer> workload = computeWorkloadMap(yearId);

        return teachingStaff().stream()
                .map(staff -> {
                    int assigned = workload.getOrDefault(staff.getStaffId(), 0);
                    int projected = assigned + weekly;
                    String status = workloadStatus(assigned, maxPeriods);
                    boolean recommended = projected <= maxPeriods;
                    String reason = recommended
                            ? (assigned <= maxPeriods * 0.75 ? "Availability: Good" : "Availability: Limited")
                            : "Would exceed weekly capacity";
                    return TeacherRecommendationResponse.builder()
                            .staffId(staff.getStaffId())
                            .staffName(staffDisplayName(staff))
                            .assignedWeeklyPeriods(assigned)
                            .maxWeeklyPeriods(maxPeriods)
                            .workloadStatus(status)
                            .recommended(recommended)
                            .reason(reason)
                            .build();
                })
                .sorted(Comparator
                        .comparing(TeacherRecommendationResponse::isRecommended).reversed()
                        .thenComparing(TeacherRecommendationResponse::getAssignedWeeklyPeriods))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TeacherWorkloadResponse getTeacherWorkload(Long staffId, Long academicYearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TEACHER_ALLOCATION);
        requireYear(academicYearId);
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + staffId));
        int maxPeriods = resolveMaxWeeklyPeriods(academicYearId);
        int assigned = computeStaffWorkload(staffId, academicYearId);
        return TeacherWorkloadResponse.builder()
                .staffId(staff.getStaffId())
                .staffName(staffDisplayName(staff))
                .academicYearId(academicYearId)
                .assignedWeeklyPeriods(assigned)
                .maxWeeklyPeriods(maxPeriods)
                .status(workloadStatus(assigned, maxPeriods))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeacherWorkloadResponse> listWorkloads(Long academicYearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TEACHER_ALLOCATION);
        requireYear(academicYearId);
        int maxPeriods = resolveMaxWeeklyPeriods(academicYearId);
        return listWorkloadsInternal(academicYearId, maxPeriods, computeWorkloadMap(academicYearId));
    }

    @Override
    public ClassTeacherAssignmentResponse assignClassTeacher(ClassTeacherAssignmentRequest request) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TEACHER_ALLOCATION);
        AcademicSection section = sectionRepository.findByIdWithClass(request.getSectionId())
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + request.getSectionId()));
        assertYearMutable(section.getAcademicClass().getAcademicYear());
        Staff staff = staffRepository.findById(request.getStaffId())
                .orElseThrow(() -> new ResourceNotFoundException("Staff not found: " + request.getStaffId()));

        classTeacherAssignmentRepository.findBySection_SectionIdAndEffectiveToIsNull(section.getSectionId())
                .ifPresent(existing -> {
                    LocalDate end = LocalDate.now();
                    if (existing.getEffectiveFrom() != null && end.isBefore(existing.getEffectiveFrom())) {
                        end = existing.getEffectiveFrom();
                    }
                    existing.setEffectiveTo(end);
                    existing.setActive(false);
                    classTeacherAssignmentRepository.save(existing);
                });

        ClassTeacherAssignment assignment = new ClassTeacherAssignment();
        assignment.setSection(section);
        assignment.setStaff(staff);
        assignment.setEffectiveFrom(request.getEffectiveFrom() == null ? LocalDate.now() : request.getEffectiveFrom());
        assignment.setActive(true);
        return toClassTeacherResponse(classTeacherAssignmentRepository.save(assignment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassTeacherAssignmentResponse> getClassTeachers(Long yearId, Long classId, Long sectionId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TEACHER_ALLOCATION);
        requireYear(yearId);
        List<AcademicSection> sections;
        if (sectionId != null) {
            AcademicSection section = sectionRepository.findByIdWithClass(sectionId)
                    .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionId));
            sections = List.of(section);
        } else if (classId != null) {
            sections = sectionRepository.findByAcademicClass_ClassIdOrderByDisplayOrderAsc(classId);
        } else {
            sections = classRepository.findByAcademicYear_AcademicYearIdOrderByDisplayOrderAsc(yearId).stream()
                    .flatMap(c -> sectionRepository.findByAcademicClass_ClassIdOrderByDisplayOrderAsc(c.getClassId()).stream())
                    .toList();
        }
        List<ClassTeacherAssignmentResponse> result = new ArrayList<>();
        for (AcademicSection section : sections) {
            classTeacherAssignmentRepository
                    .findFirstBySection_SectionIdAndActiveTrueAndEffectiveToIsNullOrderByEffectiveFromDesc(
                            section.getSectionId())
                    .ifPresent(a -> result.add(toClassTeacherResponse(a)));
        }
        return result;
    }

    @Override
    public void removeClassTeacher(Long assignmentId) {
        accessGuard.requireManage(AcademicsAccessGuard.RESOURCE_TEACHER_ALLOCATION);
        ClassTeacherAssignment assignment = classTeacherAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Class teacher assignment not found: " + assignmentId));
        AcademicSection section = sectionRepository.findByIdWithClass(assignment.getSection().getSectionId())
                .orElse(assignment.getSection());
        assertYearMutable(section.getAcademicClass().getAcademicYear());
        LocalDate end = LocalDate.now();
        if (assignment.getEffectiveFrom() != null && end.isBefore(assignment.getEffectiveFrom())) {
            end = assignment.getEffectiveFrom();
        }
        assignment.setEffectiveTo(end);
        assignment.setActive(false);
        classTeacherAssignmentRepository.save(assignment);
    }

    private List<TeacherAllocationRowResponse> buildRows(
            AcademicYear year, int maxPeriods, Map<Long, Integer> workloadByStaff) {
        List<AcademicClass> classes = classRepository
                .findWithYearByAcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(year.getAcademicYearId());
        List<TeacherAllocationRowResponse> rows = new ArrayList<>();

        for (AcademicClass academicClass : classes) {
            List<AcademicSection> sections = sectionRepository
                    .findByAcademicClass_ClassIdAndActiveTrueOrderByDisplayOrderAsc(academicClass.getClassId());
            List<ClassSubjectMapping> mappings = mappingRepository
                    .findActiveWithSubjectByClassId(academicClass.getClassId());

            for (AcademicSection section : sections) {
                for (ClassSubjectMapping mapping : mappings) {
                    TeacherAllocation allocation = allocationRepository
                            .findBySection_SectionIdAndClassSubjectMapping_ClassSubjectMappingId(
                                    section.getSectionId(), mapping.getClassSubjectMappingId())
                            .orElse(null);
                    if (allocation == null) {
                        rows.add(toVirtualRow(year, academicClass, section, mapping, maxPeriods));
                    } else {
                        rows.add(toRow(allocation, maxPeriods, workloadByStaff));
                    }
                }
            }
        }
        return rows;
    }

    private TeacherAllocationRowResponse toVirtualRow(
            AcademicYear year,
            AcademicClass academicClass,
            AcademicSection section,
            ClassSubjectMapping mapping,
            int maxPeriods) {
        return TeacherAllocationRowResponse.builder()
                .teacherAllocationId(null)
                .academicYearId(year.getAcademicYearId())
                .classId(academicClass.getClassId())
                .className(academicClass.getName())
                .classCode(academicClass.getCode())
                .sectionId(section.getSectionId())
                .sectionName(section.getName())
                .classSubjectMappingId(mapping.getClassSubjectMappingId())
                .subjectId(mapping.getSubject().getSubjectId())
                .subjectName(mapping.getSubject().getName())
                .subjectCode(mapping.getSubject().getCode())
                .subjectCategory(mapping.getSubject().getCategory())
                .weeklyPeriods(mapping.getWeeklyPeriods())
                .primaryStaffId(null)
                .primaryStaffName(null)
                .primaryWorkloadAssigned(null)
                .primaryWorkloadMax(maxPeriods)
                .primaryWorkloadStatus(null)
                .status(TeacherAllocationStatus.UNASSIGNED)
                .active(true)
                .build();
    }

    private TeacherAllocationRowResponse toRow(
            TeacherAllocation allocation, int maxPeriods, Map<Long, Integer> workloadByStaff) {
        AcademicSection section = allocation.getSection();
        AcademicClass academicClass = section.getAcademicClass();
        ClassSubjectMapping mapping = allocation.getClassSubjectMapping();

        TeacherAllocationTeacher primary = allocation.getTeacherAllocationId() == null
                ? null
                : allocationTeacherRepository
                .findFirstByTeacherAllocation_TeacherAllocationIdAndActiveTrueAndEffectiveToIsNullAndRoleOrderByEffectiveFromDesc(
                        allocation.getTeacherAllocationId(), TeacherAllocationTeacherRole.PRIMARY)
                .orElse(null);

        Long staffId = primary == null || primary.getStaff() == null ? null : primary.getStaff().getStaffId();
        Integer assigned = staffId == null ? null : workloadByStaff.getOrDefault(staffId, 0);
        // Status is always derived from the current workload; a persisted CONFLICT must not
        // survive once other allocations have been rebalanced.
        TeacherAllocationStatus status;
        if (staffId == null) {
            status = TeacherAllocationStatus.UNASSIGNED;
        } else if (assigned > maxPeriods) {
            status = TeacherAllocationStatus.CONFLICT;
        } else {
            status = TeacherAllocationStatus.ASSIGNED;
        }

        return TeacherAllocationRowResponse.builder()
                .teacherAllocationId(allocation.getTeacherAllocationId())
                .academicYearId(academicClass.getAcademicYear().getAcademicYearId())
                .classId(academicClass.getClassId())
                .className(academicClass.getName())
                .classCode(academicClass.getCode())
                .sectionId(section.getSectionId())
                .sectionName(section.getName())
                .classSubjectMappingId(mapping.getClassSubjectMappingId())
                .subjectId(mapping.getSubject().getSubjectId())
                .subjectName(mapping.getSubject().getName())
                .subjectCode(mapping.getSubject().getCode())
                .subjectCategory(mapping.getSubject().getCategory())
                .weeklyPeriods(mapping.getWeeklyPeriods())
                .primaryStaffId(staffId)
                .primaryStaffName(primary == null || primary.getStaff() == null
                        ? null
                        : staffDisplayName(primary.getStaff()))
                .primaryWorkloadAssigned(assigned)
                .primaryWorkloadMax(maxPeriods)
                .primaryWorkloadStatus(assigned == null ? null : workloadStatus(assigned, maxPeriods))
                .status(status)
                .active(allocation.getActive())
                .build();
    }

    private void closeOpenRole(TeacherAllocation allocation, TeacherAllocationTeacherRole role) {
        if (allocation.getTeacherAllocationId() == null) {
            return;
        }
        allocationTeacherRepository
                .findByTeacherAllocation_TeacherAllocationIdAndRoleAndEffectiveToIsNull(
                        allocation.getTeacherAllocationId(), role)
                .ifPresent(existing -> {
                    LocalDate end = LocalDate.now();
                    // Same-day assign+unassign must not set effective_to before effective_from
                    // (violates chk_tat_effective_dates).
                    if (existing.getEffectiveFrom() != null && end.isBefore(existing.getEffectiveFrom())) {
                        end = existing.getEffectiveFrom();
                    }
                    existing.setEffectiveTo(end);
                    existing.setActive(false);
                    allocationTeacherRepository.save(existing);
                });
    }

    private Map<Long, Integer> computeWorkloadMap(Long yearId) {
        Map<Long, Integer> map = new HashMap<>();
        for (TeacherAllocationTeacher row : allocationTeacherRepository.findActiveByYear(yearId)) {
            if (row.getRole() != TeacherAllocationTeacherRole.PRIMARY) {
                continue;
            }
            Short weekly = row.getTeacherAllocation().getClassSubjectMapping().getWeeklyPeriods();
            if (weekly == null) {
                continue;
            }
            Long staffId = row.getStaff().getStaffId();
            map.merge(staffId, weekly.intValue(), Integer::sum);
        }
        return map;
    }

    private int computeStaffWorkload(Long staffId, Long yearId) {
        return computeWorkloadMap(yearId).getOrDefault(staffId, 0);
    }

    private List<TeacherWorkloadResponse> listWorkloadsInternal(
            Long yearId, int maxPeriods, Map<Long, Integer> workloadByStaff) {
        return teachingStaff().stream()
                .map(staff -> {
                    int assigned = workloadByStaff.getOrDefault(staff.getStaffId(), 0);
                    return TeacherWorkloadResponse.builder()
                            .staffId(staff.getStaffId())
                            .staffName(staffDisplayName(staff))
                            .academicYearId(yearId)
                            .assignedWeeklyPeriods(assigned)
                            .maxWeeklyPeriods(maxPeriods)
                            .status(workloadStatus(assigned, maxPeriods))
                            .build();
                })
                .sorted(Comparator.comparing(TeacherWorkloadResponse::getAssignedWeeklyPeriods).reversed())
                .toList();
    }

    private List<Staff> teachingStaff() {
        return staffRepository.findByActiveTrueAndEmploymentStatus(EmploymentStatus.ACTIVE).stream()
                .filter(s -> s.getStaffType() == null || s.getStaffType() == StaffType.TEACHING)
                .sorted(Comparator.comparing(Staff::getFirstName).thenComparing(Staff::getLastName))
                .toList();
    }

    private int resolveMaxWeeklyPeriods(Long yearId) {
        return timetableConfigurationRepository.findByAcademicYear_AcademicYearId(yearId).stream()
                .filter(TimetableConfiguration::isActive)
                .map(TimetableConfiguration::getMaxTeacherWeeklyPeriods)
                .filter(Objects::nonNull)
                .mapToInt(Short::intValue)
                .max()
                .orElse(FALLBACK_MAX_WEEKLY_PERIODS);
    }

    private boolean hasConfiguredMax(Long yearId) {
        return timetableConfigurationRepository.findByAcademicYear_AcademicYearId(yearId).stream()
                .anyMatch(c -> c.isActive() && c.getMaxTeacherWeeklyPeriods() != null);
    }

    private String workloadStatus(int assigned, int max) {
        if (assigned > max) return "EXCEEDS_LIMIT";
        if (assigned == max) return "AT_CAPACITY";
        if (assigned >= max * 0.85) return "LIMITED";
        return "AVAILABLE";
    }

    private ClassTeacherAssignmentResponse toClassTeacherResponse(ClassTeacherAssignment assignment) {
        AcademicSection section = assignment.getSection();
        AcademicClass academicClass = section.getAcademicClass();
        return ClassTeacherAssignmentResponse.builder()
                .classTeacherAssignmentId(assignment.getClassTeacherAssignmentId())
                .sectionId(section.getSectionId())
                .sectionName(section.getName())
                .classId(academicClass == null ? null : academicClass.getClassId())
                .className(academicClass == null ? null : academicClass.getName())
                .staffId(assignment.getStaff().getStaffId())
                .staffName(staffDisplayName(assignment.getStaff()))
                .effectiveFrom(assignment.getEffectiveFrom())
                .effectiveTo(assignment.getEffectiveTo())
                .active(assignment.getActive())
                .createdOn(assignment.getCreatedOn())
                .build();
    }

    private boolean isYearReadOnly(AcademicYear year) {
        return READ_ONLY_YEAR_STATUSES.contains(year.getStatus());
    }

    private void assertYearMutable(AcademicYear year) {
        if (isYearReadOnly(year)) {
            throw new BusinessException("Historical academic years are read-only");
        }
        if (!year.isActive()) {
            throw new BusinessException("Cannot modify allocations for an inactive academic year");
        }
    }

    private AcademicYear requireYear(Long yearId) {
        return academicYearRepository.findById(yearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + yearId));
    }

    private String staffDisplayName(Staff staff) {
        String middle = StringUtils.hasText(staff.getMiddleName()) ? " " + staff.getMiddleName() : "";
        return (staff.getFirstName() + middle + " " + staff.getLastName()).trim();
    }
}
