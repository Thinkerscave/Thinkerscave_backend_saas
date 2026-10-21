package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.dto.response.TimetableReadinessResponse;
import com.thinkerscave.academics.dto.response.TimetableReadinessResponse.ReadinessCheck;
import com.thinkerscave.academics.dto.response.TimetableReadinessResponse.ReadinessSeverity;
import com.thinkerscave.academics.dto.response.TimetableReadinessResponse.ReadinessStatus;
import com.thinkerscave.academics.dto.response.TimetableReadinessResponse.ReadinessSummary;
import com.thinkerscave.academics.entity.*;
import com.thinkerscave.academics.enums.*;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData.Lesson;
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData.MissingTeacher;
import com.thinkerscave.staff.entity.Staff;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class TimetableReadinessEvaluator {

    static final String ROUTE_ACADEMIC_YEAR = "/app/academics/academic-year";
    static final String ROUTE_CLASSES = "/app/academics/classes-sections";
    static final String ROUTE_SUBJECTS = "/app/academics/subjects-mapping";
    static final String ROUTE_TEACHER_ALLOCATION = "/app/academics/teacher-allocation";
    static final String TAB_CONFIGURATION = "tab:configuration";

    private static final int MAX_DETAILS = 60;

    private static final Set<AcademicYearStatus> ELIGIBLE_STATUSES = EnumSet.of(
            AcademicYearStatus.APPROVED,
            AcademicYearStatus.CURRENT
    );

    private final TimetablePlanningDataLoader loader;
    private final GenerationProgressTracker progressTracker;

    public TimetableReadinessResponse evaluate(Long yearId) {
        return evaluate(loader.load(yearId));
    }

    public TimetableReadinessResponse evaluate(TimetablePlanningData data) {
        List<ReadinessCheck> checks = new ArrayList<>();
        AcademicYear year = data.getYear();

        if (year == null) {
            checks.add(fail("ACADEMIC_YEAR_ELIGIBLE", "Academic year", "Academic year not found", null, null, null));
            return buildResponse(checks, data, new ReadinessSummary(0, 0, 0, 0, 0, 0, 0));
        }

        checkYear(year, checks);
        boolean configUsable = checkConfiguration(data, checks);
        checkSectionsAndSubjects(data, checks);
        if (configUsable) {
            checkSectionCapacity(data, checks);
        }
        checkTeacherAllocations(data, checks);
        checkTeacherWorkload(data, checks, configUsable);
        checkResources(data, checks);
        checkRunningGeneration(data, checks);

        Set<Long> teacherIds = new HashSet<>();
        int requirementCount = 0;
        for (Lesson lesson : data.getLessons()) {
            teacherIds.add(lesson.teacher().getStaffId());
            requirementCount += lesson.weeklyPeriods();
        }
        int schedulableMappings = data.getMappings().size() - data.getZeroPeriodMappings().size();
        ReadinessSummary summary = new ReadinessSummary(
                data.getSections().size(),
                schedulableMappings,
                requirementCount,
                teacherIds.size(),
                data.getResourceCount(),
                data.weeklySlotCount(),
                data.getMissingTeachers().size());

        return buildResponse(checks, data, summary);
    }

    // ─── Checks ──────────────────────────────────────────────────────────

    private void checkYear(AcademicYear year, List<ReadinessCheck> checks) {
        boolean eligible = ELIGIBLE_STATUSES.contains(year.getStatus()) && year.isActive();
        if (eligible) {
            checks.add(pass("ACADEMIC_YEAR_ELIGIBLE", "Academic year",
                    "Academic year " + year.getName() + " is " + year.getStatus()));
        } else {
            String reason = !year.isActive()
                    ? "Academic year " + year.getName() + " is inactive."
                    : "Academic year " + year.getName() + " is " + year.getStatus()
                    + ". Timetables can be generated once the year is Approved or Current.";
            checks.add(fail("ACADEMIC_YEAR_ELIGIBLE", "Academic year", reason, null,
                    "Open Academic Year", ROUTE_ACADEMIC_YEAR));
        }
    }

    private boolean checkConfiguration(TimetablePlanningData data, List<ReadinessCheck> checks) {
        TimetableConfiguration config = data.getConfiguration();
        if (config == null) {
            checks.add(fail("TIMETABLE_CONFIGURATION_READY", "School day setup",
                    "No school day (periods and working days) is configured for this academic year.",
                    null, "Set up school day", TAB_CONFIGURATION));
            return false;
        }

        List<String> problems = new ArrayList<>();
        if (config.getSchoolStartTime() == null || config.getSchoolEndTime() == null
                || !config.getSchoolStartTime().isBefore(config.getSchoolEndTime())) {
            problems.add("School start time must be before school end time");
        }
        if (config.getMaxTeacherWeeklyPeriods() == null || config.getMaxTeacherWeeklyPeriods() <= 0) {
            problems.add("Max teacher weekly periods must be greater than 0");
        }
        if (problems.isEmpty()) {
            checks.add(pass("TIMETABLE_CONFIGURATION_READY", "School day setup",
                    "Using configuration '" + config.getName() + "' (" + config.getShiftType() + ")"));
        } else {
            checks.add(fail("TIMETABLE_CONFIGURATION_READY", "School day setup",
                    "Configuration '" + config.getName() + "' is incomplete.", problems,
                    "Fix school day", TAB_CONFIGURATION));
        }

        if (data.getActiveConfigurations().size() > 1) {
            List<String> others = data.getActiveConfigurations().stream()
                    .filter(c -> !Objects.equals(c.getTimetableConfigurationId(), config.getTimetableConfigurationId()))
                    .map(c -> "'" + c.getName() + "' (" + c.getShiftType() + ") is also active and will be ignored")
                    .toList();
            checks.add(warn("SINGLE_ACTIVE_CONFIGURATION", "Duplicate school day setups",
                    data.getActiveConfigurations().size() + " active configurations found. Generation uses '"
                            + config.getName() + "'. Saving the school day setup deactivates the others.",
                    others, "Review school day", TAB_CONFIGURATION));
        }

        boolean hasDays = !data.getWorkingDays().isEmpty();
        checks.add(hasDays
                ? pass("WORKING_DAYS_CONFIGURED", "Working days",
                data.getWorkingDays().size() + " working day(s): " + joinDays(data.getWorkingDays()))
                : fail("WORKING_DAYS_CONFIGURED", "Working days", "No working days are selected.", null,
                "Select working days", TAB_CONFIGURATION));

        List<String> invalidPeriods = new ArrayList<>();
        for (TimetablePeriod p : data.getAllPeriods()) {
            if (p.getStartTime() == null || p.getEndTime() == null || !p.getStartTime().isBefore(p.getEndTime())) {
                invalidPeriods.add(periodLabel(p) + " must end after it starts");
            }
        }
        boolean hasTeaching = !data.getTeachingPeriods().isEmpty();
        if (!hasTeaching) {
            checks.add(fail("TEACHING_PERIODS_VALID", "Teaching periods",
                    "No teaching periods are configured.", invalidPeriods, "Add periods", TAB_CONFIGURATION));
        } else if (!invalidPeriods.isEmpty()) {
            checks.add(fail("TEACHING_PERIODS_VALID", "Teaching periods",
                    invalidPeriods.size() + " period(s) have invalid times.", invalidPeriods,
                    "Fix periods", TAB_CONFIGURATION));
        } else {
            checks.add(pass("TEACHING_PERIODS_VALID", "Teaching periods",
                    data.getTeachingPeriods().size() + " teaching period(s) per day"));
        }

        List<String> overlaps = new ArrayList<>();
        List<TimetablePeriod> periods = data.getAllPeriods().stream()
                .filter(p -> p.getStartTime() != null && p.getEndTime() != null)
                .toList();
        for (int i = 0; i < periods.size(); i++) {
            for (int j = i + 1; j < periods.size(); j++) {
                TimetablePeriod a = periods.get(i);
                TimetablePeriod b = periods.get(j);
                if (a.getStartTime().isBefore(b.getEndTime()) && b.getStartTime().isBefore(a.getEndTime())) {
                    overlaps.add(periodLabel(a) + " overlaps " + periodLabel(b));
                }
            }
        }
        checks.add(overlaps.isEmpty()
                ? pass("PERIODS_NO_OVERLAP", "Period timings", "Period times do not overlap")
                : fail("PERIODS_NO_OVERLAP", "Period timings", overlaps.size() + " period overlap(s) found.",
                overlaps, "Fix periods", TAB_CONFIGURATION));

        List<String> outOfBounds = new ArrayList<>();
        LocalTime start = config.getSchoolStartTime();
        LocalTime end = config.getSchoolEndTime();
        if (start != null && end != null) {
            for (TimetablePeriod p : periods) {
                if (p.getStartTime().isBefore(start) || p.getEndTime().isAfter(end)) {
                    outOfBounds.add(periodLabel(p) + " is outside school hours " + start + "–" + end);
                }
            }
        }
        checks.add(outOfBounds.isEmpty()
                ? pass("PERIODS_WITHIN_SCHOOL_BOUNDS", "School hours", "All periods are within school hours")
                : fail("PERIODS_WITHIN_SCHOOL_BOUNDS", "School hours",
                outOfBounds.size() + " period(s) fall outside school hours.", outOfBounds,
                "Fix periods", TAB_CONFIGURATION));

        return problems.isEmpty() && hasDays && hasTeaching && invalidPeriods.isEmpty()
                && overlaps.isEmpty() && outOfBounds.isEmpty();
    }

    private void checkSectionsAndSubjects(TimetablePlanningData data, List<ReadinessCheck> checks) {
        int sectionCount = data.getSections().size();
        checks.add(sectionCount > 0
                ? pass("ACTIVE_SECTIONS_PRESENT", "Classes & sections", sectionCount + " active section(s)")
                : fail("ACTIVE_SECTIONS_PRESENT", "Classes & sections",
                "No active classes with sections exist for this academic year.", null,
                "Open Classes & Sections", ROUTE_CLASSES));

        int schedulable = data.getMappings().size() - data.getZeroPeriodMappings().size();
        if (schedulable == 0) {
            checks.add(fail("SUBJECT_MAPPINGS_PRESENT", "Class subjects",
                    "No subjects with weekly periods are mapped to classes.", null,
                    "Map subjects", ROUTE_SUBJECTS));
        } else {
            Set<Long> classesWithSubjects = new HashSet<>();
            for (ClassSubjectMapping m : data.getMappings()) {
                if (m.getWeeklyPeriods() != null && m.getWeeklyPeriods() > 0) {
                    classesWithSubjects.add(m.getAcademicClass().getClassId());
                }
            }
            List<String> sectionsWithoutSubjects = data.getSections().stream()
                    .filter(s -> !classesWithSubjects.contains(s.getAcademicClass().getClassId()))
                    .map(s -> TimetablePlanningData.sectionLabel(s) + " has no subjects mapped (it will have an empty timetable)")
                    .toList();
            if (sectionsWithoutSubjects.isEmpty()) {
                checks.add(pass("SUBJECT_MAPPINGS_PRESENT", "Class subjects",
                        schedulable + " class-subject mapping(s) with weekly periods"));
            } else {
                checks.add(warn("SUBJECT_MAPPINGS_PRESENT", "Class subjects",
                        sectionsWithoutSubjects.size() + " section(s) have no subjects mapped.",
                        sectionsWithoutSubjects, "Map subjects", ROUTE_SUBJECTS));
            }
        }

        if (!data.getZeroPeriodMappings().isEmpty()) {
            List<String> details = data.getZeroPeriodMappings().stream()
                    .map(m -> m.getAcademicClass().getName() + " · " + m.getSubject().getName()
                            + " has 0 weekly periods (skipped)")
                    .toList();
            checks.add(warn("WEEKLY_PERIODS_POSITIVE", "Weekly periods",
                    details.size() + " class subject(s) have no weekly periods and will be skipped.",
                    details, "Set weekly periods", ROUTE_SUBJECTS));
        } else {
            checks.add(pass("WEEKLY_PERIODS_POSITIVE", "Weekly periods",
                    "All mapped subjects have weekly periods"));
        }
    }

    private void checkSectionCapacity(TimetablePlanningData data, List<ReadinessCheck> checks) {
        int slots = data.weeklySlotCount();
        Map<Long, Integer> requiredByClass = new HashMap<>();
        for (ClassSubjectMapping m : data.getMappings()) {
            if (m.getWeeklyPeriods() != null && m.getWeeklyPeriods() > 0) {
                requiredByClass.merge(m.getAcademicClass().getClassId(), (int) m.getWeeklyPeriods(), Integer::sum);
            }
        }
        List<String> over = new ArrayList<>();
        for (AcademicSection s : data.getSections()) {
            int required = requiredByClass.getOrDefault(s.getAcademicClass().getClassId(), 0);
            if (required > slots) {
                over.add(TimetablePlanningData.sectionLabel(s) + " needs " + required
                        + " periods/week but only " + slots + " teaching slots exist");
            }
        }
        checks.add(over.isEmpty()
                ? pass("SECTION_CAPACITY", "Weekly capacity",
                "Every section fits within " + slots + " teaching slots per week")
                : fail("SECTION_CAPACITY", "Weekly capacity",
                over.size() + " section(s) need more periods than the school week has. "
                        + "Add teaching periods/working days or reduce weekly periods.",
                over, "Adjust school day", TAB_CONFIGURATION));
    }

    private void checkTeacherAllocations(TimetablePlanningData data, List<ReadinessCheck> checks) {
        List<MissingTeacher> missing = data.getMissingTeachers();
        if (missing.isEmpty()) {
            if (!data.getLessons().isEmpty()) {
                checks.add(pass("TEACHER_ALLOCATION_COMPLETE", "Teacher allocation",
                        "Every mapped subject in every section has an active teacher"));
            }
            return;
        }
        List<String> details = missing.stream()
                .map(m -> TimetablePlanningData.sectionLabel(m.section()) + " · " + m.mapping().getSubject().getName()
                        + " — " + m.reason().getLabel()
                        + (m.teacherName() != null ? " (" + m.teacherName() + ")" : "")
                        + ", " + m.weeklyPeriods() + " period(s) will stay free")
                .toList();
        if (data.getLessons().isEmpty()) {
            checks.add(fail("TEACHER_ALLOCATION_COMPLETE", "Teacher allocation",
                    "No subject has an active teacher assigned yet.", details,
                    "Assign teachers", ROUTE_TEACHER_ALLOCATION));
        } else {
            int periods = missing.stream().mapToInt(MissingTeacher::weeklyPeriods).sum();
            checks.add(warn("TEACHER_ALLOCATION_COMPLETE", "Teacher allocation",
                    missing.size() + " class subject(s) have no active teacher. Their " + periods
                            + " weekly period(s) will be left free until a teacher is assigned.",
                    details, "Assign teachers", ROUTE_TEACHER_ALLOCATION));
        }
    }

    private void checkTeacherWorkload(TimetablePlanningData data, List<ReadinessCheck> checks, boolean configUsable) {
        TimetableConfiguration config = data.getConfiguration();
        if (config == null || config.getMaxTeacherWeeklyPeriods() == null || data.getLessons().isEmpty()) {
            return;
        }
        int maxAllowed = config.getMaxTeacherWeeklyPeriods();
        int slots = data.weeklySlotCount();

        Map<Long, Integer> load = new LinkedHashMap<>();
        Map<Long, Staff> staffById = new HashMap<>();
        for (Lesson lesson : data.getLessons()) {
            load.merge(lesson.teacher().getStaffId(), lesson.weeklyPeriods(), Integer::sum);
            staffById.put(lesson.teacher().getStaffId(), lesson.teacher());
        }

        List<String> over = new ArrayList<>();
        load.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .forEach(e -> {
                    String name = TimetablePlanningData.staffName(staffById.get(e.getKey()));
                    if (e.getValue() > maxAllowed) {
                        over.add(name + " — " + e.getValue() + " periods/week (limit " + maxAllowed + ")");
                    } else if (configUsable && e.getValue() > slots) {
                        over.add(name + " — " + e.getValue() + " periods/week but the week only has " + slots + " slots");
                    }
                });

        checks.add(over.isEmpty()
                ? pass("TEACHER_WORKLOAD_WITHIN_LIMIT", "Teacher workload",
                load.size() + " teacher(s), all within " + maxAllowed + " periods/week")
                : fail("TEACHER_WORKLOAD_WITHIN_LIMIT", "Teacher workload",
                over.size() + " teacher(s) are over their weekly limit. Reassign some subjects or raise "
                        + "'Max teacher weekly periods'.",
                over, "Rebalance allocation", ROUTE_TEACHER_ALLOCATION));
    }

    private void checkResources(TimetablePlanningData data, List<ReadinessCheck> checks) {
        checks.add(pass("RESOURCES_AVAILABLE", "Rooms",
                data.getResourceCount() > 0
                        ? data.getResourceCount() + " active room(s); sections with a default room are checked for room clashes"
                        : "No rooms configured (optional) — room clashes are not checked"));
    }

    private void checkRunningGeneration(TimetablePlanningData data, List<ReadinessCheck> checks) {
        boolean running = data.getVersions().stream()
                .anyMatch(v -> v.getGenerationStatus() == TimetableGenerationStatus.GENERATING
                        && progressTracker.hasActive(v.getTimetableVersionId()));
        checks.add(running
                ? fail("NO_ACTIVE_GENERATION_RUNNING", "Generation in progress",
                "A timetable generation is already running for this year. Wait for it to finish.",
                null, null, null)
                : pass("NO_ACTIVE_GENERATION_RUNNING", "Generation in progress", "No generation is running"));
    }

    // ─── Builders ─────────────────────────────────────────────────────────

    private TimetableReadinessResponse buildResponse(List<ReadinessCheck> checks,
                                                     TimetablePlanningData data,
                                                     ReadinessSummary summary) {
        List<ReadinessCheck> blocking = checks.stream()
                .filter(c -> c.getSeverity() == ReadinessSeverity.BLOCKING && c.getStatus() == ReadinessStatus.FAILED)
                .toList();
        List<ReadinessCheck> warnings = checks.stream()
                .filter(c -> c.getStatus() == ReadinessStatus.WARNING)
                .toList();
        TimetableConfiguration config = data.getConfiguration();
        return TimetableReadinessResponse.builder()
                .ready(blocking.isEmpty())
                .summary(summary)
                .checks(checks)
                .blockingIssues(blocking)
                .warnings(warnings)
                .configurationId(config == null ? null : config.getTimetableConfigurationId())
                .configurationName(config == null ? null : config.getName())
                .build();
    }

    private static ReadinessCheck pass(String code, String title, String message) {
        return ReadinessCheck.builder()
                .code(code).title(title)
                .status(ReadinessStatus.PASSED)
                .severity(ReadinessSeverity.BLOCKING)
                .message(message)
                .details(List.of())
                .build();
    }

    private static ReadinessCheck fail(String code, String title, String message, List<String> details,
                                       String actionLabel, String actionRoute) {
        return ReadinessCheck.builder()
                .code(code).title(title)
                .status(ReadinessStatus.FAILED)
                .severity(ReadinessSeverity.BLOCKING)
                .message(message)
                .details(cap(details))
                .actionLabel(actionLabel)
                .actionRoute(actionRoute)
                .build();
    }

    private static ReadinessCheck warn(String code, String title, String message, List<String> details,
                                       String actionLabel, String actionRoute) {
        return ReadinessCheck.builder()
                .code(code).title(title)
                .status(ReadinessStatus.WARNING)
                .severity(ReadinessSeverity.WARNING)
                .message(message)
                .details(cap(details))
                .actionLabel(actionLabel)
                .actionRoute(actionRoute)
                .build();
    }

    private static List<String> cap(List<String> details) {
        if (details == null || details.isEmpty()) return List.of();
        if (details.size() <= MAX_DETAILS) return details;
        List<String> capped = new ArrayList<>(details.subList(0, MAX_DETAILS));
        capped.add("…and " + (details.size() - MAX_DETAILS) + " more");
        return capped;
    }

    private static String periodLabel(TimetablePeriod p) {
        return p.getName() + " (" + p.getStartTime() + "–" + p.getEndTime() + ")";
    }

    private static String joinDays(List<DayOfWeek> days) {
        return String.join(", ", days.stream()
                .map(d -> d.name().charAt(0) + d.name().substring(1, 3).toLowerCase())
                .toList());
    }
}
