package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.response.AcademicsOverviewResponse;
import com.thinkerscave.academics.dto.response.AcademicsOverviewResponse.*;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.AcademicYearStatus;
import com.thinkerscave.academics.enums.TeacherAllocationStatus;
import com.thinkerscave.academics.enums.TimetableConflictStatus;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.academics.repository.*;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.AcademicsOverviewService;
import com.thinkerscave.shared.exceptions.BusinessException;
import com.thinkerscave.student.repository.StudentEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AcademicsOverviewServiceImpl implements AcademicsOverviewService {

    private final AcademicsAccessGuard accessGuard;
    private final AcademicYearRepository yearRepo;
    private final ClassRepository classRepo;
    private final SectionRepository sectionRepo;
    private final SubjectRepository subjectRepo;
    private final ClassSubjectMappingRepository mappingRepo;
    private final TeacherAllocationRepository allocationRepo;
    private final TimetableVersionRepository versionRepo;
    private final TimetableConflictRepository conflictRepo;
    private final StudentEnrollmentRepository enrollmentRepo;

    @Override
    public AcademicsOverviewResponse getOverview(Long yearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_OVERVIEW);

        AcademicYear year = yearRepo.findById(yearId)
                .orElseThrow(() -> new BusinessException("Academic year not found"));

        List<ReadinessStep> readinessSteps = buildReadinessSteps(yearId, year);
        List<ClassCard> topClasses = buildTopClasses(yearId);
        long totalStudents = 0;
        try {
            totalStudents = enrollmentRepo.countByAcademicYearAcademicYearIdAndActiveTrue(yearId);
        } catch (Exception ignored) {
            // enrollment count may not be available
        }

        return AcademicsOverviewResponse.builder()
                .yearHeader(buildYearHeader(year))
                .structureCounts(buildStructureCounts(yearId, totalStudents))
                .mapping(buildMappingSummary(yearId))
                .allocation(buildAllocationSummary(yearId))
                .timetable(buildTimetableSummary(yearId))
                .readinessSteps(readinessSteps)
                .alerts(buildAlerts(yearId))
                .topClasses(topClasses)
                .topSubjects(buildTopSubjects(yearId))
                .setupCompletePercent(computeSetupPercent(readinessSteps))
                .importantDates(buildImportantDates(year))
                .studentsByClass(buildStudentsByClass(topClasses, totalStudents))
                .build();
    }

    private YearHeader buildYearHeader(AcademicYear year) {
        boolean readOnly = year.getStatus() == AcademicYearStatus.COMPLETED
                || year.getStatus() == AcademicYearStatus.ARCHIVED;

        int progress = 0;
        long daysCompleted = 0;
        long daysRemaining = 0;
        long totalDays = 0;
        LocalDate today = LocalDate.now();

        if (year.getStartDate() != null && year.getEndDate() != null) {
            totalDays = Math.max(1, ChronoUnit.DAYS.between(year.getStartDate(), year.getEndDate()) + 1);
            if (!today.isBefore(year.getStartDate())) {
                LocalDate capped = today.isAfter(year.getEndDate()) ? year.getEndDate() : today;
                daysCompleted = ChronoUnit.DAYS.between(year.getStartDate(), capped) + 1;
                progress = (int) Math.min(100, (daysCompleted * 100) / totalDays);
            }
            if (today.isAfter(year.getEndDate())) {
                daysRemaining = 0;
            } else if (today.isBefore(year.getStartDate())) {
                // Not started yet: remaining = full year length (align with progress bar / Academic Year page)
                daysRemaining = totalDays;
            } else {
                daysRemaining = Math.max(0, totalDays - daysCompleted);
            }
        }

        return YearHeader.builder()
                .academicYearId(year.getAcademicYearId())
                .name(year.getName())
                .status(year.getStatus())
                .startDate(year.getStartDate())
                .endDate(year.getEndDate())
                .yearReadOnly(readOnly)
                .progressPercent(progress)
                .daysCompleted(daysCompleted)
                .daysRemaining(daysRemaining)
                .totalDays(totalDays)
                .build();
    }

    private StructureCounts buildStructureCounts(Long yearId, long studentsActive) {
        return StructureCounts.builder()
                .classesActive(classRepo.countByAcademicYear_AcademicYearIdAndActiveTrue(yearId))
                .classesTotal(classRepo.countByAcademicYear_AcademicYearId(yearId))
                .sectionsActive(sectionRepo.countByAcademicClass_AcademicYear_AcademicYearIdAndActiveTrue(yearId))
                .sectionsTotal(sectionRepo.countByAcademicClass_AcademicYear_AcademicYearId(yearId))
                .subjectsActive(subjectRepo.countByAcademicYear_AcademicYearIdAndActiveTrue(yearId))
                .subjectsTotal(subjectRepo.countByAcademicYear_AcademicYearId(yearId))
                .studentsActive(studentsActive)
                .build();
    }

    private MappingSummary buildMappingSummary(Long yearId) {
        long subjectsTotal = subjectRepo.countByAcademicYear_AcademicYearIdAndActiveTrue(yearId);
        long unmapped = mappingRepo.countUnmappedActiveSubjects(yearId);
        long mapped = Math.max(0, subjectsTotal - unmapped);

        return MappingSummary.builder()
                .subjectsMapped(mapped)
                .subjectsTotal(subjectsTotal)
                .pendingMappings(unmapped)
                .build();
    }

    private AllocationSummary buildAllocationSummary(Long yearId) {
        List<TeacherAllocation> allocations = allocationRepo
                .findBySection_AcademicClass_AcademicYear_AcademicYearId(yearId);
        long total = allocations.size();
        long assigned = allocations.stream()
                .filter(a -> a.getStatus() == TeacherAllocationStatus.ASSIGNED)
                .count();

        return AllocationSummary.builder()
                .assignedSlots(assigned)
                .totalSlots(total)
                .missingSlots(total - assigned)
                .build();
    }

    private TimetableSummary buildTimetableSummary(Long yearId) {
        List<TimetableVersion> versions = versionRepo
                .findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(yearId);

        Integer publishedVer = null;
        Integer latestVer = null;
        var genStatus = com.thinkerscave.academics.enums.TimetableGenerationStatus.NOT_GENERATED;
        TimetableStatus ttStatus = null;
        long blocking = 0;

        if (!versions.isEmpty()) {
            TimetableVersion latest = versions.get(0);
            latestVer = latest.getVersionNumber();
            genStatus = latest.getGenerationStatus();
            ttStatus = latest.getStatus();

            blocking = conflictRepo.findByTimetableVersion_TimetableVersionIdAndBlockingTrueAndStatus(
                    latest.getTimetableVersionId(), TimetableConflictStatus.OPEN).size();

            Optional<TimetableVersion> pub = versions.stream()
                    .filter(v -> v.getStatus() == TimetableStatus.PUBLISHED)
                    .findFirst();
            if (pub.isPresent()) {
                publishedVer = pub.get().getVersionNumber();
            }
        }

        return TimetableSummary.builder()
                .publishedVersion(publishedVer)
                .latestVersion(latestVer)
                .generationStatus(genStatus)
                .status(ttStatus)
                .openBlockingConflicts(blocking)
                .build();
    }

    private List<ReadinessStep> buildReadinessSteps(Long yearId, AcademicYear year) {
        List<ReadinessStep> steps = new ArrayList<>();
        long classesActive = classRepo.countByAcademicYear_AcademicYearIdAndActiveTrue(yearId);
        long sectionsActive = sectionRepo.countByAcademicClass_AcademicYear_AcademicYearIdAndActiveTrue(yearId);
        long subjectsActive = subjectRepo.countByAcademicYear_AcademicYearIdAndActiveTrue(yearId);
        long unmappedSubjects = mappingRepo.countUnmappedActiveSubjects(yearId);

        List<TeacherAllocation> allocs = allocationRepo
                .findBySection_AcademicClass_AcademicYear_AcademicYearId(yearId);
        long unassigned = allocs.stream()
                .filter(a -> a.getStatus() == TeacherAllocationStatus.UNASSIGNED)
                .count();

        boolean hasPublished = versionRepo
                .findByAcademicYear_AcademicYearIdAndStatus(yearId, TimetableStatus.PUBLISHED)
                .isPresent();

        steps.add(ReadinessStep.builder()
                .code("YEAR").label("Academic Year")
                .state(year.getStatus() == AcademicYearStatus.CURRENT
                        || year.getStatus() == AcademicYearStatus.APPROVED
                        || year.getStatus() == AcademicYearStatus.PREPARING
                        || year.getStatus() == AcademicYearStatus.PENDING_APPROVAL
                        ? "COMPLETE" : "PENDING")
                .detail(year.getStatus() != null ? year.getStatus().name() : "")
                .build());

        steps.add(ReadinessStep.builder()
                .code("CLASSES").label("Classes & Sections")
                .state(classesActive > 0 && sectionsActive > 0 ? "COMPLETE" : "PENDING")
                .detail(classesActive + " classes, " + sectionsActive + " sections")
                .build());

        steps.add(ReadinessStep.builder()
                .code("SUBJECTS").label("Subjects")
                .state(subjectsActive > 0 ? "COMPLETE" : "PENDING")
                .detail(subjectsActive + " active subjects")
                .build());

        steps.add(ReadinessStep.builder()
                .code("MAPPING").label("Subject Mapping")
                .state(unmappedSubjects == 0 && subjectsActive > 0 ? "COMPLETE" : "IN_PROGRESS")
                .detail(unmappedSubjects + " subjects pending mapping")
                .build());

        steps.add(ReadinessStep.builder()
                .code("ALLOCATION").label("Teacher Allocation")
                .state(unassigned == 0 && !allocs.isEmpty() ? "COMPLETE"
                        : (!allocs.isEmpty() ? "IN_PROGRESS" : "PENDING"))
                .detail(unassigned + " slots unassigned")
                .build());

        steps.add(ReadinessStep.builder()
                .code("TIMETABLE").label("Timetable")
                .state(hasPublished ? "COMPLETE" : "IN_PROGRESS")
                .detail(hasPublished ? "Published" : "Not yet published")
                .build());

        return steps;
    }

    private int computeSetupPercent(List<ReadinessStep> steps) {
        if (steps == null || steps.isEmpty()) {
            return 0;
        }
        long complete = steps.stream().filter(s -> "COMPLETE".equalsIgnoreCase(s.getState())).count();
        return (int) Math.round((complete * 100.0) / steps.size());
    }

    private List<ImportantDate> buildImportantDates(AcademicYear year) {
        List<ImportantDate> dates = new ArrayList<>();
        if (year.getStartDate() != null) {
            dates.add(ImportantDate.builder()
                    .code("YEAR_START").label("Academic Year Start")
                    .date(year.getStartDate()).build());
        }
        if (year.getEndDate() != null) {
            dates.add(ImportantDate.builder()
                    .code("YEAR_END").label("Academic Year End")
                    .date(year.getEndDate()).build());
            dates.add(ImportantDate.builder()
                    .code("NEXT_YEAR_START").label("Next Academic Year Start")
                    .date(year.getEndDate().plusDays(1)).build());
        }
        return dates;
    }

    private List<OverviewAlert> buildAlerts(Long yearId) {
        List<OverviewAlert> alerts = new ArrayList<>();
        long unmapped = mappingRepo.countUnmappedActiveSubjects(yearId);
        if (unmapped > 0) {
            alerts.add(OverviewAlert.builder()
                    .severity("WARNING").code("UNMAPPED_SUBJECTS")
                    .message(unmapped + " subject(s) not mapped to any class")
                    .route("/app/academics/subjects-mapping")
                    .build());
        }

        List<TeacherAllocation> allocs = allocationRepo
                .findBySection_AcademicClass_AcademicYear_AcademicYearId(yearId);
        long missing = allocs.stream()
                .filter(a -> a.getStatus() == TeacherAllocationStatus.UNASSIGNED)
                .count();
        if (missing > 0) {
            alerts.add(OverviewAlert.builder()
                    .severity("WARNING").code("MISSING_ALLOCATIONS")
                    .message(missing + " teacher allocation slot(s) unassigned")
                    .route("/app/academics/teacher-allocation")
                    .build());
        }

        List<TimetableVersion> versions = versionRepo
                .findByAcademicYear_AcademicYearIdOrderByVersionNumberDesc(yearId);
        if (!versions.isEmpty()) {
            long blocking = conflictRepo.findByTimetableVersion_TimetableVersionIdAndBlockingTrueAndStatus(
                    versions.get(0).getTimetableVersionId(), TimetableConflictStatus.OPEN).size();
            if (blocking > 0) {
                alerts.add(OverviewAlert.builder()
                        .severity("ERROR").code("TIMETABLE_CONFLICTS")
                        .message(blocking + " open timetable conflict(s) need resolution")
                        .route("/app/academics/timetable")
                        .build());
            }
        }

        return alerts;
    }

    private List<ClassCard> buildTopClasses(Long yearId) {
        List<AcademicClass> classes = classRepo
                .findByAcademicYear_AcademicYearIdAndActiveTrueOrderByDisplayOrderAsc(yearId);
        return classes.stream()
                .limit(6)
                .map(c -> {
                    long studentCount = 0;
                    try {
                        studentCount = enrollmentRepo.countByClassEntityClassIdAndActiveTrue(c.getClassId());
                    } catch (Exception ignored) {
                        // ignore
                    }
                    return ClassCard.builder()
                            .classId(c.getClassId())
                            .className(c.getName())
                            .sectionCount(sectionRepo.countByAcademicClass_ClassIdAndActiveTrue(c.getClassId()))
                            .studentCount(studentCount)
                            .classTeacherName(null)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<SubjectCard> buildTopSubjects(Long yearId) {
        List<Subject> subjects = subjectRepo
                .findByAcademicYear_AcademicYearIdAndActiveTrueOrderByNameAsc(yearId);
        List<TeacherAllocation> allocs = allocationRepo
                .findBySection_AcademicClass_AcademicYear_AcademicYearId(yearId);

        return subjects.stream()
                .limit(6)
                .map(s -> {
                    long mappedClasses = mappingRepo.countBySubject_SubjectIdAndActiveTrue(s.getSubjectId());
                    boolean anyAssigned = allocs.stream()
                            .filter(a -> a.getClassSubjectMapping() != null
                                    && a.getClassSubjectMapping().getSubject() != null
                                    && Objects.equals(a.getClassSubjectMapping().getSubject().getSubjectId(),
                                    s.getSubjectId()))
                            .anyMatch(a -> a.getStatus() == TeacherAllocationStatus.ASSIGNED);
                    boolean anySlot = allocs.stream()
                            .anyMatch(a -> a.getClassSubjectMapping() != null
                                    && a.getClassSubjectMapping().getSubject() != null
                                    && Objects.equals(a.getClassSubjectMapping().getSubject().getSubjectId(),
                                    s.getSubjectId()));
                    String teacherStatus = !anySlot ? "-" : (anyAssigned ? "Assigned" : "Pending");
                    return SubjectCard.builder()
                            .subjectId(s.getSubjectId())
                            .subjectName(s.getName())
                            .code(s.getCode())
                            .category(s.getCategory() != null ? s.getCategory().name() : null)
                            .mappedClassCount(mappedClasses)
                            .defaultWeeklyPeriods(s.getDefaultWeeklyPeriods())
                            .teacherStatus(teacherStatus)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<StudentsByClass> buildStudentsByClass(List<ClassCard> topClasses, long totalStudents) {
        if (topClasses == null || topClasses.isEmpty()) {
            return Collections.emptyList();
        }
        long shown = topClasses.stream()
                .mapToLong(c -> c.getStudentCount() != null ? c.getStudentCount() : 0L)
                .sum();
        List<StudentsByClass> rows = new ArrayList<>();
        for (ClassCard c : topClasses) {
            long count = c.getStudentCount() != null ? c.getStudentCount() : 0L;
            double percent = totalStudents > 0 ? (count * 100.0) / totalStudents : 0;
            rows.add(StudentsByClass.builder()
                    .classId(c.getClassId())
                    .className(c.getClassName())
                    .studentCount(count)
                    .percent(Math.round(percent * 10.0) / 10.0)
                    .build());
        }
        if (totalStudents > shown) {
            long others = totalStudents - shown;
            double percent = totalStudents > 0 ? (others * 100.0) / totalStudents : 0;
            rows.add(StudentsByClass.builder()
                    .classId(null)
                    .className("Others")
                    .studentCount(others)
                    .percent(Math.round(percent * 10.0) / 10.0)
                    .build());
        }
        return rows;
    }
}
