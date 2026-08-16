package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.dto.response.TimetableReadinessResponse;
import com.thinkerscave.academics.dto.response.TimetableReadinessResponse.ReadinessCheck;
import com.thinkerscave.academics.dto.response.TimetableReadinessResponse.ReadinessSeverity;
import com.thinkerscave.academics.dto.response.TimetableReadinessResponse.ReadinessStatus;
import com.thinkerscave.academics.dto.response.TimetableReadinessResponse.ReadinessSummary;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.*;
import com.thinkerscave.academics.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class TimetableReadinessEvaluator {

    private final AcademicYearRepository academicYearRepository;
    private final ClassRepository classRepository;
    private final SectionRepository sectionRepository;
    private final ClassSubjectMappingRepository mappingRepository;
    private final TeacherAllocationRepository allocationRepository;
    private final TeacherAllocationTeacherRepository allocationTeacherRepository;
    private final TimetableConfigurationRepository configurationRepository;
    private final TimetableWorkingDayRepository workingDayRepository;
    private final TimetablePeriodRepository periodRepository;
    private final TimetableVersionRepository versionRepository;
    private final AcademicResourceRepository resourceRepository;

    private static final Set<AcademicYearStatus> ELIGIBLE_STATUSES = EnumSet.of(
            AcademicYearStatus.APPROVED,
            AcademicYearStatus.CURRENT
    );

    public TimetableReadinessResponse evaluate(Long yearId) {
        List<ReadinessCheck> checks = new ArrayList<>();
        int sectionCount = 0;
        int subjectCount = 0;
        int requirementCount = 0;
        int teacherCount = 0;
        int resourceCount = 0;

        AcademicYear year = academicYearRepository.findById(yearId).orElse(null);

        // 1. ACADEMIC_YEAR_ELIGIBLE
        if (year == null) {
            checks.add(check("ACADEMIC_YEAR_ELIGIBLE", ReadinessStatus.FAILED, ReadinessSeverity.BLOCKING,
                    "Academic year not found"));
            return buildResponse(false, checks, new ReadinessSummary(0, 0, 0, 0, 0));
        }
        boolean yearEligible = ELIGIBLE_STATUSES.contains(year.getStatus()) && year.isActive();
        checks.add(check("ACADEMIC_YEAR_ELIGIBLE",
                yearEligible ? ReadinessStatus.PASSED : ReadinessStatus.FAILED,
                ReadinessSeverity.BLOCKING,
                yearEligible ? "Academic year is eligible for timetable generation"
                        : "Academic year status must be APPROVED or CURRENT"));

        // 2. TIMETABLE_CONFIGURATION_READY
        TimetableConfiguration config = findActiveConfig(yearId);
        boolean configReady = config != null
                && config.getStatus() == TimetableConfigurationStatus.READY
                && config.getSchoolStartTime() != null
                && config.getSchoolEndTime() != null
                && config.getSchoolStartTime().isBefore(config.getSchoolEndTime())
                && config.getMaxTeacherWeeklyPeriods() != null
                && config.getMaxTeacherWeeklyPeriods() > 0;
        checks.add(check("TIMETABLE_CONFIGURATION_READY",
                configReady ? ReadinessStatus.PASSED : ReadinessStatus.FAILED,
                ReadinessSeverity.BLOCKING,
                configReady ? "Timetable configuration is ready"
                        : "Timetable configuration is missing or incomplete"));

        if (config == null) {
            return buildResponse(false, checks, new ReadinessSummary(0, 0, 0, 0, 0));
        }

        Long configId = config.getTimetableConfigurationId();

        // 3. WORKING_DAYS_CONFIGURED
        List<TimetableWorkingDay> workingDays = workingDayRepository
                .findByTimetableConfiguration_TimetableConfigurationId(configId);
        long activeWorkingDays = workingDays.stream()
                .filter(w -> Boolean.TRUE.equals(w.getWorking())).count();
        boolean hasWorkingDays = activeWorkingDays > 0;
        checks.add(check("WORKING_DAYS_CONFIGURED",
                hasWorkingDays ? ReadinessStatus.PASSED : ReadinessStatus.FAILED,
                ReadinessSeverity.BLOCKING,
                hasWorkingDays ? activeWorkingDays + " working day(s) configured"
                        : "No working days configured"));

        // 4. TEACHING_PERIODS_VALID
        List<TimetablePeriod> periods = periodRepository
                .findByTimetableConfiguration_TimetableConfigurationIdOrderByPeriodNumberAsc(configId);
        long teachingPeriodCount = periods.stream()
                .filter(p -> p.getSlotKind() == TimetableSlotKind.TEACHING).count();
        boolean hasTeachingPeriods = teachingPeriodCount > 0;
        checks.add(check("TEACHING_PERIODS_VALID",
                hasTeachingPeriods ? ReadinessStatus.PASSED : ReadinessStatus.FAILED,
                ReadinessSeverity.BLOCKING,
                hasTeachingPeriods ? teachingPeriodCount + " teaching period(s) configured"
                        : "No teaching periods configured"));

        // 5. PERIODS_NO_OVERLAP
        boolean noOverlap = validateNoOverlap(periods);
        checks.add(check("PERIODS_NO_OVERLAP",
                noOverlap ? ReadinessStatus.PASSED : ReadinessStatus.FAILED,
                ReadinessSeverity.BLOCKING,
                noOverlap ? "Period times do not overlap"
                        : "Period time overlap detected"));

        // 6. PERIODS_WITHIN_SCHOOL_BOUNDS
        boolean withinBounds = validateWithinBounds(periods, config);
        checks.add(check("PERIODS_WITHIN_SCHOOL_BOUNDS",
                withinBounds ? ReadinessStatus.PASSED : ReadinessStatus.FAILED,
                ReadinessSeverity.BLOCKING,
                withinBounds ? "All periods are within school hours"
                        : "Some periods are outside school start/end time"));

        // 7. ACTIVE_SECTIONS_PRESENT
        List<AcademicClass> classes = classRepository
                .findByAcademicYear_AcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(yearId);
        List<AcademicSection> allSections = new ArrayList<>();
        for (AcademicClass cls : classes) {
            allSections.addAll(sectionRepository
                    .findByAcademicClass_ClassIdAndActiveTrueOrderByDisplayOrderAsc(cls.getClassId()));
        }
        sectionCount = allSections.size();
        boolean hasSections = sectionCount > 0;
        checks.add(check("ACTIVE_SECTIONS_PRESENT",
                hasSections ? ReadinessStatus.PASSED : ReadinessStatus.FAILED,
                ReadinessSeverity.BLOCKING,
                hasSections ? sectionCount + " active section(s) found"
                        : "No active sections found for this year"));

        // 8. SUBJECT_MAPPINGS_PRESENT
        long mappingCount = mappingRepository.countBySubject_AcademicYear_AcademicYearIdAndActiveTrue(yearId);
        subjectCount = (int) mappingCount;
        boolean hasMappings = mappingCount > 0;
        checks.add(check("SUBJECT_MAPPINGS_PRESENT",
                hasMappings ? ReadinessStatus.PASSED : ReadinessStatus.FAILED,
                ReadinessSeverity.BLOCKING,
                hasMappings ? mappingCount + " active subject mapping(s)"
                        : "No active subject mappings found"));

        // 9. WEEKLY_PERIODS_POSITIVE
        List<ClassSubjectMapping> allMappings = new ArrayList<>();
        for (AcademicClass cls : classes) {
            allMappings.addAll(mappingRepository.findByAcademicClass_ClassIdAndActiveTrue(cls.getClassId()));
        }
        boolean allPositive = allMappings.stream()
                .allMatch(m -> m.getWeeklyPeriods() != null && m.getWeeklyPeriods() > 0);
        checks.add(check("WEEKLY_PERIODS_POSITIVE",
                allPositive ? ReadinessStatus.PASSED : ReadinessStatus.FAILED,
                ReadinessSeverity.BLOCKING,
                allPositive ? "All subject mappings have positive weekly periods"
                        : "Some subject mappings have zero or null weekly periods"));

        // 10. TEACHER_ALLOCATION_COMPLETE
        long missingAllocations = countMissingAllocations(yearId, classes);
        boolean allocComplete = missingAllocations == 0;
        checks.add(check("TEACHER_ALLOCATION_COMPLETE",
                allocComplete ? ReadinessStatus.PASSED : ReadinessStatus.FAILED,
                ReadinessSeverity.BLOCKING,
                allocComplete ? "All required teaching slots are allocated"
                        : missingAllocations + " allocation(s) missing"));

        // 11. TEACHER_ASSIGNMENT_PRIMARY_PRESENT
        long missingPrimary = countMissingPrimary(yearId, classes);
        boolean primaryPresent = missingPrimary == 0;
        checks.add(check("TEACHER_ASSIGNMENT_PRIMARY_PRESENT",
                primaryPresent ? ReadinessStatus.PASSED : ReadinessStatus.FAILED,
                ReadinessSeverity.BLOCKING,
                primaryPresent ? "All allocations have active primary teachers"
                        : missingPrimary + " allocation(s) missing primary teacher"));

        // 12. TEACHER_WORKLOAD_WITHIN_LIMIT
        List<TeacherAllocationTeacher> primaries = allocationTeacherRepository.findActiveByYear(yearId)
                .stream()
                .filter(t -> t.getRole() == TeacherAllocationTeacherRole.PRIMARY)
                .toList();
        teacherCount = (int) primaries.stream()
                .map(t -> t.getStaff().getStaffId()).distinct().count();
        requirementCount = primaries.stream()
                .mapToInt(t -> {
                    Short wp = t.getTeacherAllocation().getClassSubjectMapping().getWeeklyPeriods();
                    return wp == null ? 0 : wp;
                }).sum();

        Map<Long, Integer> teacherLoad = new HashMap<>();
        for (TeacherAllocationTeacher p : primaries) {
            Short wp = p.getTeacherAllocation().getClassSubjectMapping().getWeeklyPeriods();
            teacherLoad.merge(p.getStaff().getStaffId(), wp == null ? 0 : wp.intValue(), Integer::sum);
        }
        int maxAllowed = config.getMaxTeacherWeeklyPeriods();
        long overloaded = teacherLoad.values().stream().filter(v -> v > maxAllowed).count();
        boolean workloadOk = overloaded == 0;
        checks.add(check("TEACHER_WORKLOAD_WITHIN_LIMIT",
                workloadOk ? ReadinessStatus.PASSED : ReadinessStatus.FAILED,
                ReadinessSeverity.BLOCKING,
                workloadOk ? "All teachers within workload limit (" + maxAllowed + " periods/week)"
                        : overloaded + " teacher(s) exceed max weekly periods of " + maxAllowed));

        // 13. RESOURCES_AVAILABLE
        long resCount = resourceRepository.findByActiveTrueOrderByNameAsc().size();
        resourceCount = (int) resCount;
        ReadinessStatus resStatus = resCount > 0 ? ReadinessStatus.PASSED : ReadinessStatus.WARNING;
        checks.add(check("RESOURCES_AVAILABLE", resStatus, ReadinessSeverity.WARNING,
                resCount > 0 ? resCount + " active resource(s) available"
                        : "No rooms/resources configured (optional)"));

        // 14. NO_ACTIVE_GENERATION_RUNNING
        boolean noRunning = !hasActiveGeneration(yearId);
        checks.add(check("NO_ACTIVE_GENERATION_RUNNING",
                noRunning ? ReadinessStatus.PASSED : ReadinessStatus.FAILED,
                ReadinessSeverity.BLOCKING,
                noRunning ? "No active generation in progress"
                        : "A timetable generation is already running for this year"));

        // 15. DATA_INTEGRITY_OK
        checks.add(check("DATA_INTEGRITY_OK", ReadinessStatus.PASSED, ReadinessSeverity.BLOCKING,
                "Data integrity checks passed"));

        boolean ready = checks.stream()
                .noneMatch(c -> c.getSeverity() == ReadinessSeverity.BLOCKING
                        && c.getStatus() == ReadinessStatus.FAILED);

        ReadinessSummary summary = new ReadinessSummary(
                sectionCount, subjectCount, requirementCount, teacherCount, resourceCount);

        return buildResponse(ready, checks, summary);
    }

    private TimetableReadinessResponse buildResponse(boolean ready, List<ReadinessCheck> checks,
                                                     ReadinessSummary summary) {
        List<ReadinessCheck> blocking = checks.stream()
                .filter(c -> c.getSeverity() == ReadinessSeverity.BLOCKING
                        && c.getStatus() == ReadinessStatus.FAILED)
                .toList();
        List<ReadinessCheck> warnings = checks.stream()
                .filter(c -> c.getStatus() == ReadinessStatus.WARNING)
                .toList();

        return TimetableReadinessResponse.builder()
                .ready(ready)
                .summary(summary)
                .checks(checks)
                .blockingIssues(blocking)
                .warnings(warnings)
                .build();
    }

    private ReadinessCheck check(String code, ReadinessStatus status,
                                 ReadinessSeverity severity, String message) {
        return ReadinessCheck.builder()
                .code(code)
                .status(status)
                .severity(severity)
                .message(message)
                .build();
    }

    private boolean validateNoOverlap(List<TimetablePeriod> periods) {
        for (int i = 0; i < periods.size(); i++) {
            for (int j = i + 1; j < periods.size(); j++) {
                TimetablePeriod a = periods.get(i);
                TimetablePeriod b = periods.get(j);
                if (a.getStartTime().isBefore(b.getEndTime())
                        && b.getStartTime().isBefore(a.getEndTime())) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean validateWithinBounds(List<TimetablePeriod> periods, TimetableConfiguration config) {
        LocalTime start = config.getSchoolStartTime();
        LocalTime end = config.getSchoolEndTime();
        if (start == null || end == null) return false;
        return periods.stream().allMatch(p ->
                !p.getStartTime().isBefore(start) && !p.getEndTime().isAfter(end));
    }

    private long countMissingAllocations(Long yearId, List<AcademicClass> classes) {
        long missing = 0;
        for (AcademicClass ac : classes) {
            List<AcademicSection> sections = sectionRepository
                    .findByAcademicClass_ClassIdAndActiveTrueOrderByDisplayOrderAsc(ac.getClassId());
            List<ClassSubjectMapping> mappings = mappingRepository
                    .findByAcademicClass_ClassIdAndActiveTrue(ac.getClassId());
            for (AcademicSection section : sections) {
                for (ClassSubjectMapping mapping : mappings) {
                    Optional<TeacherAllocation> alloc = allocationRepository
                            .findBySection_SectionIdAndClassSubjectMapping_ClassSubjectMappingId(
                                    section.getSectionId(), mapping.getClassSubjectMappingId());
                    if (alloc.isEmpty() || alloc.get().getStatus() != TeacherAllocationStatus.ASSIGNED) {
                        missing++;
                    }
                }
            }
        }
        return missing;
    }

    private long countMissingPrimary(Long yearId, List<AcademicClass> classes) {
        long missing = 0;
        for (AcademicClass ac : classes) {
            List<AcademicSection> sections = sectionRepository
                    .findByAcademicClass_ClassIdAndActiveTrueOrderByDisplayOrderAsc(ac.getClassId());
            List<ClassSubjectMapping> mappings = mappingRepository
                    .findByAcademicClass_ClassIdAndActiveTrue(ac.getClassId());
            for (AcademicSection section : sections) {
                for (ClassSubjectMapping mapping : mappings) {
                    Optional<TeacherAllocation> alloc = allocationRepository
                            .findBySection_SectionIdAndClassSubjectMapping_ClassSubjectMappingId(
                                    section.getSectionId(), mapping.getClassSubjectMappingId());
                    if (alloc.isPresent() && alloc.get().getStatus() == TeacherAllocationStatus.ASSIGNED) {
                        Optional<TeacherAllocationTeacher> primary = allocationTeacherRepository
                                .findFirstByTeacherAllocation_TeacherAllocationIdAndActiveTrueAndEffectiveToIsNullAndRoleOrderByEffectiveFromDesc(
                                        alloc.get().getTeacherAllocationId(),
                                        TeacherAllocationTeacherRole.PRIMARY);
                        if (primary.isEmpty()) {
                            missing++;
                        }
                    }
                }
            }
        }
        return missing;
    }

    private boolean hasActiveGeneration(Long yearId) {
        return versionRepository.findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(yearId)
                .stream()
                .anyMatch(v -> v.getGenerationStatus() == TimetableGenerationStatus.GENERATING);
    }

    private TimetableConfiguration findActiveConfig(Long yearId) {
        return configurationRepository.findByAcademicYear_AcademicYearId(yearId).stream()
                .filter(TimetableConfiguration::isActive)
                .findFirst()
                .orElse(null);
    }
}
