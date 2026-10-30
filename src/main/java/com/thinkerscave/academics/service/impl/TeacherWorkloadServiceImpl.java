package com.thinkerscave.academics.service.impl;

import com.thinkerscave.academics.dto.response.TeacherWorkloadDashboardResponse;
import com.thinkerscave.academics.dto.response.TeacherWorkloadDashboardResponse.Breakdown;
import com.thinkerscave.academics.dto.response.TeacherWorkloadDashboardResponse.Conflict;
import com.thinkerscave.academics.dto.response.TeacherWorkloadDashboardResponse.FilterOption;
import com.thinkerscave.academics.dto.response.TeacherWorkloadDashboardResponse.Row;
import com.thinkerscave.academics.dto.response.TeacherWorkloadDashboardResponse.Summary;
import com.thinkerscave.academics.dto.response.TeacherWorkloadDashboardResponse.UnassignedSubject;
import com.thinkerscave.academics.dto.response.TeacherWorkloadDashboardResponse.Warning;
import com.thinkerscave.academics.entity.AcademicGroup;
import com.thinkerscave.academics.entity.AcademicGroupChoiceMapping;
import com.thinkerscave.academics.entity.AcademicGroupSubjectMapping;
import com.thinkerscave.academics.entity.AcademicYear;
import com.thinkerscave.academics.entity.FixedActivity;
import com.thinkerscave.academics.entity.TimetableConflict;
import com.thinkerscave.academics.entity.TimetableEntry;
import com.thinkerscave.academics.entity.TimetablePeriod;
import com.thinkerscave.academics.entity.TimetableVersion;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.SubjectType;
import com.thinkerscave.academics.enums.TeacherWorkloadStatus;
import com.thinkerscave.academics.enums.TimetableConflictStatus;
import com.thinkerscave.academics.enums.TimetableEntryType;
import com.thinkerscave.academics.enums.TimetableSlotKind;
import com.thinkerscave.academics.enums.TimetableStatus;
import com.thinkerscave.academics.repository.AcademicYearRepository;
import com.thinkerscave.academics.repository.TimetableConflictRepository;
import com.thinkerscave.academics.repository.TimetableEntryRepository;
import com.thinkerscave.academics.security.AcademicsAccessGuard;
import com.thinkerscave.academics.service.TeacherWorkloadService;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData.Lesson;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData.MissingTeacher;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData.Unit;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningDataLoader;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.staff.entity.Staff;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.StaffType;
import com.thinkerscave.staff.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Read-only Teacher Workload view.
 *
 * <p>Capacity comes from the persisted timetable schedule pattern assigned to each
 * academic group/section. The benchmark is the highest valid weekly teaching capacity
 * across units, so differing group patterns do not silently lower the target.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeacherWorkloadServiceImpl implements TeacherWorkloadService {

    private static final String BASIS_ALLOCATION = "ALLOCATION_ESTIMATE";
    private static final String TARGET_BASIS = "SCHEDULE_PATTERN_BENCHMARK";
    private static final String TARGET_EXPLANATION =
            "Estimated target calculated as 80% of the weekly teaching-period capacity from the "
                    + "active timetable schedule pattern. The remaining 20% is a planning buffer.";
    private static final String TARGET_UNAVAILABLE_EXPLANATION =
            "Workload target unavailable. Configure a valid timetable schedule pattern with teaching "
                    + "periods for this academic year.";

    private final AcademicYearRepository academicYearRepository;
    private final StaffRepository staffRepository;
    private final TimetablePlanningDataLoader planningDataLoader;
    private final TimetableEntryRepository timetableEntryRepository;
    private final TimetableConflictRepository timetableConflictRepository;
    private final AcademicsAccessGuard accessGuard;

    @Override
    public TeacherWorkloadDashboardResponse getDashboard(Long academicYearId) {
        accessGuard.requireView(AcademicsAccessGuard.RESOURCE_TEACHER_ALLOCATION);
        AcademicYear year = requireYear(academicYearId);
        TimetablePlanningData data = planningDataLoader.load(academicYearId);

        int benchmark = benchmarkCapacity(data);
        boolean targetAvailable = benchmark > 0;
        Integer target = TeacherWorkloadCalculator.expectedTarget(benchmark);
        Integer maximum = TeacherWorkloadCalculator.maximum(benchmark);
        Integer nearMaximumThreshold = TeacherWorkloadCalculator.nearMaximumThreshold(maximum);

        Map<Long, Staff> staffById = eligibleStaff();
        Map<Long, StaffAccumulator> workload = new HashMap<>();
        Set<String> countedLessons = new HashSet<>();

        accumulateAllocationLessons(data, staffById, workload, countedLessons);
        accumulateFixedActivities(data, staffById, workload, allocatedSubjectKeys(data));

        List<UnassignedSubject> unassignedSubjects = unassignedSubjects(data, countedLessons);
        Set<Long> unassignedSubjectIds = new HashSet<>();
        unassignedSubjects.forEach(item -> unassignedSubjectIds.add(item.getSubjectId()));

        TimetableVersion version = resolveVersion(data.getVersions());
        ScheduledSnapshot scheduled = loadScheduledSnapshot(version);

        List<Row> rows = new ArrayList<>();
        for (Staff staff : staffById.values()) {
            StaffAccumulator accumulator = workload.get(staff.getStaffId());
            int assigned = accumulator == null ? 0 : accumulator.totalPeriods;
            Integer remaining = TeacherWorkloadCalculator.remainingCapacity(target, assigned);
            TeacherWorkloadStatus status = TeacherWorkloadCalculator.classify(assigned, target, maximum);
            List<Conflict> conflicts = scheduled.conflictsByStaff.getOrDefault(staff.getStaffId(), List.of());

            rows.add(Row.builder()
                    .staffId(staff.getStaffId())
                    .staffName(staffDisplayName(staff))
                    .employeeId(staff.getStaffCode())
                    .email(staff.getEmail())
                    .assignedPeriods(assigned)
                    .expectedTarget(target)
                    .remainingCapacity(remaining)
                    .maximumPeriods(maximum)
                    .overloadPeriods(TeacherWorkloadCalculator.overloadPeriods(assigned, maximum))
                    .status(status)
                    .workloadBasis(BASIS_ALLOCATION)
                    .scheduledPeriods(scheduled.periodsByStaff.get(staff.getStaffId()))
                    .timetableConflicts(conflicts.size())
                    .subjects(accumulator == null ? List.of() : accumulator.subjectNames())
                    .breakdown(accumulator == null ? List.of() : accumulator.breakdown())
                    .conflicts(conflicts)
                    .build());
        }
        rows.sort(Comparator.comparing(Row::getStaffName, String.CASE_INSENSITIVE_ORDER));

        Summary summary = Summary.builder()
                .totalTeachers(rows.size())
                .overloaded((int) rows.stream().filter(r -> r.getStatus() == TeacherWorkloadStatus.OVERLOADED).count())
                .nearMaximum((int) rows.stream().filter(r -> r.getStatus() == TeacherWorkloadStatus.NEAR_MAXIMUM).count())
                .atTarget((int) rows.stream().filter(r -> r.getStatus() == TeacherWorkloadStatus.AT_TARGET).count())
                .belowTarget((int) rows.stream().filter(r -> r.getStatus() == TeacherWorkloadStatus.BELOW_TARGET).count())
                .unassignedSubjects(unassignedSubjectIds.size())
                .build();

        return TeacherWorkloadDashboardResponse.builder()
                .academicYearId(year.getAcademicYearId())
                .academicYearName(year.getName())
                .academicYearStatus(year.getStatus() == null ? null : year.getStatus().name())
                .targetAvailable(targetAvailable)
                .benchmarkWeeklyCapacity(targetAvailable ? benchmark : null)
                .expectedTarget(target)
                .maximumWeeklyCapacity(maximum)
                .nearMaximumThreshold(nearMaximumThreshold)
                .bufferPercentage(TeacherWorkloadCalculator.BUFFER_PERCENTAGE)
                .targetBasis(targetAvailable ? TARGET_BASIS : "UNAVAILABLE")
                .targetExplanation(targetAvailable
                        ? TARGET_EXPLANATION
                        : TARGET_UNAVAILABLE_EXPLANATION)
                .workloadBasis(BASIS_ALLOCATION)
                .timetableStatus(version == null ? "NOT_GENERATED" : version.getStatus().name())
                .timetableVersionId(version == null ? null : version.getTimetableVersionId())
                .timetableVersionNumber(version == null ? null : version.getVersionNumber())
                .timetableVersionStatus(version == null || version.getStatus() == null ? null : version.getStatus().name())
                .timetableConflictCount(scheduled.openConflictCount)
                .summary(summary)
                .warnings(buildWarnings(data, targetAvailable, benchmark, unassignedSubjects))
                .rows(rows)
                .subjectOptions(subjectOptions(data))
                .academicGroupOptions(groupOptions(data))
                .unassignedSubjects(unassignedSubjects)
                .build();
    }

    // ─── Capacity ────────────────────────────────────────────────────────────

    private int benchmarkCapacity(TimetablePlanningData data) {
        int benchmark = 0;
        for (Unit unit : data.getUnits()) {
            Map<DayOfWeek, List<TimetablePeriod>> schedule = data.getScheduleByUnit().get(unit.unitId());
            benchmark = Math.max(benchmark, TeacherWorkloadCalculator.weeklyTeachingCapacity(schedule));
        }
        return benchmark;
    }

    // ─── Workload ────────────────────────────────────────────────────────────

    private Map<Long, Staff> eligibleStaff() {
        Map<Long, Staff> staffById = new LinkedHashMap<>();
        staffRepository.findByActiveTrueAndEmploymentStatus(EmploymentStatus.ACTIVE).stream()
                .filter(staff -> staff.getStaffType() == null || staff.getStaffType() == StaffType.TEACHING)
                .sorted(Comparator.comparing(Staff::getFirstName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(Staff::getLastName, String.CASE_INSENSITIVE_ORDER))
                .forEach(staff -> staffById.put(staff.getStaffId(), staff));
        return staffById;
    }

    private void accumulateAllocationLessons(
            TimetablePlanningData data,
            Map<Long, Staff> staffById,
            Map<Long, StaffAccumulator> workload,
            Set<String> countedLessons) {
        for (Lesson lesson : data.getLessons()) {
            Staff teacher = lesson.teacher();
            if (teacher == null || teacher.getStaffId() == null) {
                continue;
            }
            staffById.putIfAbsent(teacher.getStaffId(), teacher);

            AcademicGroupSubjectMapping mapping = lesson.mapping();
            AcademicGroupChoiceMapping choice = mapping.getAcademicGroupChoiceMapping();
            String dedupeKey = lessonKey(teacher.getStaffId(), lesson.unit().unitId(), mapping, choice);
            int periods = lesson.weeklyPeriods();

            StaffAccumulator accumulator = workload.computeIfAbsent(
                    teacher.getStaffId(), ignored -> new StaffAccumulator());
            if (!countedLessons.add(dedupeKey)) {
                accumulator.mergeChoiceSubject(dedupeKey, mapping.getSubject().getName(), periods);
                continue;
            }
            accumulator.add(Breakdown.builder()
                    .subjectId(mapping.getSubject().getSubjectId())
                    .subjectName(mapping.getSubject().getName())
                    .subjectCode(mapping.getSubject().getCode())
                    .academicGroupId(lesson.unit().academicGroupId())
                    .academicGroupName(lesson.unit().academicGroupName())
                    .sectionId(lesson.unit().sectionId())
                    .sectionName(lesson.unit().sectionName())
                    .weeklyPeriods(periods)
                    .source(choice == null ? "ALLOCATION" : "CHOICE_GROUP")
                    .choiceGroupName(choice == null || choice.getChoiceGroup() == null
                            ? null : choice.getChoiceGroup().getName())
                    .build(), dedupeKey);
        }
    }

    /**
     * Activity-type fixed lessons add load only when the teacher has no allocation for that
     * subject in the unit. When an allocation exists it already contains the fixed lesson, so
     * counting both would report the same period twice. Slots shared by several units (a
     * whole-school assembly) are counted once, because they are one teaching period.
     */
    private void accumulateFixedActivities(
            TimetablePlanningData data,
            Map<Long, Staff> staffById,
            Map<Long, StaffAccumulator> workload,
            Set<String> allocatedSubjectKeys) {
        for (FixedActivity activity : data.getFixedActivities()) {
            if (!activity.isActive() || activity.getStaff() == null || activity.getSubject() == null) {
                continue;
            }
            if (isAcademicSubject(activity.getSubject().getSubjectType())) {
                continue;
            }
            if (activity.getDays() == null || activity.getDays().isEmpty()) {
                continue;
            }
            Staff teacher = activity.getStaff();
            staffById.putIfAbsent(teacher.getStaffId(), teacher);
            StaffAccumulator accumulator = workload.computeIfAbsent(
                    teacher.getStaffId(), ignored -> new StaffAccumulator());
            Set<String> countedSlots = new HashSet<>();

            for (Unit unit : data.getUnits()) {
                if (!data.fixedActivitiesFor(unit).contains(activity)) {
                    continue;
                }
                String allocationKey = teacher.getStaffId() + "|" + unit.unitId() + "|"
                        + activity.getSubject().getSubjectId();
                if (allocatedSubjectKeys.contains(allocationKey)) {
                    continue;
                }
                int uniqueSlots = 0;
                for (DayOfWeek day : activity.getDays()) {
                    var resolved = activity.periodFor(
                            unit.group().getAcademicGroupId(), unit.sectionId(), day);
                    String slot = day + "|" + (resolved == null || resolved.getTimetablePeriod() == null
                            ? "unresolved" : resolved.getTimetablePeriod().getTimetablePeriodId());
                    if (countedSlots.add(slot)) {
                        uniqueSlots++;
                    }
                }
                if (uniqueSlots == 0) {
                    continue;
                }
                String key = "FIXED|" + teacher.getStaffId() + "|" + unit.unitId() + "|"
                        + activity.getFixedActivityId();
                accumulator.add(Breakdown.builder()
                        .subjectId(activity.getSubject().getSubjectId())
                        .subjectName(activity.getSubject().getName())
                        .subjectCode(activity.getSubject().getCode())
                        .academicGroupId(unit.academicGroupId())
                        .academicGroupName(unit.academicGroupName())
                        .sectionId(unit.sectionId())
                        .sectionName(unit.sectionName())
                        .weeklyPeriods(uniqueSlots)
                        .source("FIXED_ACTIVITY")
                        .build(), key);
            }
        }
    }

    private static Set<String> allocatedSubjectKeys(TimetablePlanningData data) {
        Set<String> keys = new HashSet<>();
        for (Lesson lesson : data.getLessons()) {
            if (lesson.teacher() == null || lesson.teacher().getStaffId() == null) continue;
            keys.add(lesson.teacher().getStaffId() + "|" + lesson.unit().unitId() + "|"
                    + lesson.mapping().getSubject().getSubjectId());
        }
        return keys;
    }

    private static String lessonKey(
            Long staffId,
            Long unitId,
            AcademicGroupSubjectMapping mapping,
            AcademicGroupChoiceMapping choice) {
        if (choice != null && choice.getAcademicGroupChoiceMappingId() != null) {
            // Alternative subjects in one choice group represent the same teaching slot.
            return staffId + "|" + unitId + "|CHOICE|" + choice.getAcademicGroupChoiceMappingId();
        }
        return staffId + "|" + unitId + "|MAPPING|" + mapping.getAcademicGroupSubjectMappingId();
    }

    // ─── Unassigned subjects ─────────────────────────────────────────────────

    private List<UnassignedSubject> unassignedSubjects(
            TimetablePlanningData data, Set<String> countedLessons) {
        Set<String> satisfiedChoiceUnits = new HashSet<>();
        for (Lesson lesson : data.getLessons()) {
            AcademicGroupChoiceMapping choice = lesson.mapping().getAcademicGroupChoiceMapping();
            if (choice != null && choice.getAcademicGroupChoiceMappingId() != null) {
                satisfiedChoiceUnits.add(lesson.unit().unitId() + "|" + choice.getAcademicGroupChoiceMappingId());
            }
        }

        List<UnassignedSubject> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (MissingTeacher missing : data.getMissingTeachers()) {
            AcademicGroupSubjectMapping mapping = missing.mapping();
            AcademicGroupChoiceMapping choice = mapping.getAcademicGroupChoiceMapping();
            if (choice != null && choice.getAcademicGroupChoiceMappingId() != null
                    && satisfiedChoiceUnits.contains(
                            missing.unit().unitId() + "|" + choice.getAcademicGroupChoiceMappingId())) {
                // Another alternative in this choice group has a teacher, so the slot is covered.
                continue;
            }
            String key = mapping.getSubject().getSubjectId() + "|" + missing.unit().unitId();
            if (!seen.add(key)) {
                continue;
            }
            result.add(UnassignedSubject.builder()
                    .subjectId(mapping.getSubject().getSubjectId())
                    .subjectName(mapping.getSubject().getName())
                    .academicGroupId(missing.unit().academicGroupId())
                    .academicGroupName(missing.unit().academicGroupName())
                    .sectionId(missing.unit().sectionId())
                    .sectionName(missing.unit().sectionName())
                    .weeklyPeriods(missing.weeklyPeriods())
                    .reason(missing.reason().getLabel())
                    .build());
        }
        result.sort(Comparator
                .comparing(UnassignedSubject::getAcademicGroupName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(UnassignedSubject::getSectionName,
                        Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                .thenComparing(UnassignedSubject::getSubjectName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    // ─── Timetable snapshot ──────────────────────────────────────────────────

    private TimetableVersion resolveVersion(List<TimetableVersion> versions) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }
        return versions.stream()
                .filter(v -> v.getStatus() == TimetableStatus.PUBLISHED)
                .findFirst()
                .orElse(versions.get(0));
    }

    private ScheduledSnapshot loadScheduledSnapshot(TimetableVersion version) {
        if (version == null) {
            return new ScheduledSnapshot();
        }
        List<TimetableEntry> entries = timetableEntryRepository.findGridEntries(version.getTimetableVersionId());
        Map<Long, Set<String>> slotsByStaff = new HashMap<>();
        for (TimetableEntry entry : entries) {
            if (entry.getStaff() == null || entry.getStaff().getStaffId() == null) {
                continue;
            }
            if (entry.getEntryType() == TimetableEntryType.FREE_PERIOD) {
                continue;
            }
            String slot = entry.getDayOfWeek() + "|"
                    + (entry.getTimetablePeriod() == null ? "?" : entry.getTimetablePeriod().getTimetablePeriodId());
            slotsByStaff.computeIfAbsent(entry.getStaff().getStaffId(), ignored -> new HashSet<>()).add(slot);
        }

        Map<Long, Integer> periodsByStaff = new HashMap<>();
        slotsByStaff.forEach((staffId, slots) -> periodsByStaff.put(staffId, slots.size()));

        Map<Long, List<Conflict>> conflictsByStaff = new HashMap<>();
        int openConflicts = 0;
        for (TimetableConflict conflict : timetableConflictRepository.findDetailedByVersion(
                version.getTimetableVersionId())) {
            if (conflict.getStatus() != TimetableConflictStatus.OPEN) {
                continue;
            }
            openConflicts++;
            Conflict dto = toConflict(conflict);
            for (Long staffId : conflictStaffIds(conflict)) {
                conflictsByStaff.computeIfAbsent(staffId, ignored -> new ArrayList<>()).add(dto);
            }
        }

        ScheduledSnapshot snapshot = new ScheduledSnapshot();
        snapshot.periodsByStaff = periodsByStaff;
        snapshot.conflictsByStaff = conflictsByStaff;
        snapshot.openConflictCount = openConflicts;
        return snapshot;
    }

    private Set<Long> conflictStaffIds(TimetableConflict conflict) {
        Set<Long> ids = new LinkedHashSet<>();
        addStaff(ids, conflict.getTimetableEntry());
        addStaff(ids, conflict.getRelatedTimetableEntry());
        return ids;
    }

    private void addStaff(Set<Long> ids, TimetableEntry entry) {
        if (entry != null && entry.getStaff() != null && entry.getStaff().getStaffId() != null) {
            ids.add(entry.getStaff().getStaffId());
        }
    }

    private Conflict toConflict(TimetableConflict conflict) {
        return Conflict.builder()
                .conflictId(conflict.getTimetableConflictId())
                .conflictType(conflict.getConflictType() == null ? null : conflict.getConflictType().name())
                .message(conflict.getMessage())
                .blocking(Boolean.TRUE.equals(conflict.getBlocking()))
                .status(conflict.getStatus() == null ? null : conflict.getStatus().name())
                .dayOfWeek(conflict.getDayOfWeek() == null ? null : conflict.getDayOfWeek().name())
                .periodName(conflict.getTimetablePeriod() == null ? null : conflict.getTimetablePeriod().getName())
                .sectionName(conflict.getSection() == null ? null : conflict.getSection().getName())
                .build();
    }

    // ─── Options and warnings ────────────────────────────────────────────────

    private List<FilterOption> subjectOptions(TimetablePlanningData data) {
        Map<Long, String> subjects = new LinkedHashMap<>();
        for (AcademicGroupSubjectMapping mapping : data.getMappings()) {
            subjects.putIfAbsent(mapping.getSubject().getSubjectId(), mapping.getSubject().getName());
        }
        return subjects.entrySet().stream()
                .map(entry -> FilterOption.builder().id(entry.getKey()).label(entry.getValue()).build())
                .sorted(Comparator.comparing(FilterOption::getLabel, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<FilterOption> groupOptions(TimetablePlanningData data) {
        Map<Long, String> groups = new LinkedHashMap<>();
        for (Unit unit : data.getUnits()) {
            groups.putIfAbsent(unit.academicGroupId(), unit.academicGroupName());
        }
        return groups.entrySet().stream()
                .map(entry -> FilterOption.builder().id(entry.getKey()).label(entry.getValue()).build())
                .sorted(Comparator.comparing(FilterOption::getLabel, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private List<Warning> buildWarnings(
            TimetablePlanningData data,
            boolean targetAvailable,
            int benchmark,
            List<UnassignedSubject> unassignedSubjects) {
        List<Warning> warnings = new ArrayList<>();
        if (!targetAvailable) {
            warnings.add(Warning.builder()
                    .code("TARGET_UNAVAILABLE")
                    .severity("WARNING")
                    .message(TARGET_UNAVAILABLE_EXPLANATION)
                    .build());
        } else {
            long unitsWithoutSchedule = data.getUnits().stream()
                    .filter(unit -> TeacherWorkloadCalculator.weeklyTeachingCapacity(
                            data.getScheduleByUnit().get(unit.unitId())) == 0)
                    .count();
            if (unitsWithoutSchedule > 0) {
                warnings.add(Warning.builder()
                        .code("PARTIAL_SCHEDULE_PATTERN")
                        .severity("INFO")
                        .message(unitsWithoutSchedule + " group/section unit(s) have no valid schedule pattern. "
                                + "The highest valid capacity (" + benchmark + " teaching periods/week) is used "
                                + "as the temporary school-wide benchmark.")
                        .build());
            }
        }

        boolean hasSchedulableMapping = data.getMappings().stream()
                .anyMatch(m -> m.getWeeklyPeriods() != null && m.getWeeklyPeriods() > 0);
        if (!hasSchedulableMapping) {
            warnings.add(Warning.builder()
                    .code("NO_CURRICULUM_MAPPINGS")
                    .severity("WARNING")
                    .message("No curriculum mapping with weekly periods exists for this academic year, so "
                            + "assigned workload cannot be calculated.")
                    .build());
        }

        if (!data.getMissingTeachers().isEmpty()) {
            warnings.add(Warning.builder()
                    .code("INCOMPLETE_ALLOCATIONS")
                    .severity("WARNING")
                    .message(data.getMissingTeachers().size() + " curriculum allocation slot(s) have no active "
                            + "teacher. Workload totals are allocation-based estimates and may be incomplete.")
                    .build());
        }

        if (!unassignedSubjects.isEmpty()) {
            warnings.add(Warning.builder()
                    .code("UNASSIGNED_SUBJECTS")
                    .severity("INFO")
                    .message(unassignedSubjects.size() + " subject/group/section slot(s) still require a teacher. "
                            + "Use Subject Allocation to complete them.")
                    .build());
        }
        return warnings;
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private AcademicYear requireYear(Long yearId) {
        return academicYearRepository.findById(yearId)
                .orElseThrow(() -> new ResourceNotFoundException("Academic year not found: " + yearId));
    }

    private static boolean isAcademicSubject(SubjectType type) {
        return type == SubjectType.ACADEMIC_SUBJECT || type == SubjectType.PRACTICAL_LAB;
    }

    private String staffDisplayName(Staff staff) {
        String middle = StringUtils.hasText(staff.getMiddleName()) ? " " + staff.getMiddleName() : "";
        return (staff.getFirstName() + middle + " " + staff.getLastName()).trim();
    }

    private static final class ScheduledSnapshot {
        private Map<Long, Integer> periodsByStaff = new HashMap<>();
        private Map<Long, List<Conflict>> conflictsByStaff = new HashMap<>();
        private int openConflictCount;
    }

    private static final class StaffAccumulator {
        private int totalPeriods;
        private final Map<String, Breakdown> rowsByKey = new LinkedHashMap<>();
        private final Set<String> subjects = new LinkedHashSet<>();

        void add(Breakdown breakdown, String key) {
            if (rowsByKey.putIfAbsent(key, breakdown) == null) {
                totalPeriods += breakdown.getWeeklyPeriods();
                if (StringUtils.hasText(breakdown.getSubjectName())) {
                    subjects.add(breakdown.getSubjectName());
                }
            }
        }

        void mergeChoiceSubject(String key, String subjectName, int weeklyPeriods) {
            Breakdown existing = rowsByKey.get(key);
            if (existing == null || !StringUtils.hasText(subjectName)) {
                return;
            }
            if (weeklyPeriods > existing.getWeeklyPeriods()) {
                totalPeriods += weeklyPeriods - existing.getWeeklyPeriods();
                existing.setWeeklyPeriods(weeklyPeriods);
            }
            String current = existing.getSubjectName();
            if (current == null) {
                existing.setSubjectName(subjectName);
            } else if (List.of(current.split(" / ")).stream()
                    .noneMatch(name -> name.equalsIgnoreCase(subjectName))) {
                existing.setSubjectName(current + " / " + subjectName);
            }
            subjects.add(subjectName);
        }

        List<Breakdown> breakdown() {
            return rowsByKey.values().stream()
                    .sorted(Comparator
                            .comparing(Breakdown::getAcademicGroupName, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(Breakdown::getSectionName,
                                    Comparator.nullsFirst(String.CASE_INSENSITIVE_ORDER))
                            .thenComparing(Breakdown::getSubjectName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
        }

        List<String> subjectNames() {
            return subjects.stream()
                    .filter(StringUtils::hasText)
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }
    }
}
