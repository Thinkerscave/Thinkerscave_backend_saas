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
import com.thinkerscave.academics.timetable.engine.TimetablePlanningData.Unit;
import com.thinkerscave.staff.entity.Staff;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

@Component
@RequiredArgsConstructor
public class TimetableReadinessEvaluator {

    static final String ROUTE_ACADEMIC_YEAR = "/app/academics/academic-year";
    static final String ROUTE_CLASSES = "/app/academics/academic-groups";
    static final String ROUTE_SUBJECTS = "/app/academics/subject-mapping";
    static final String ROUTE_TEACHER_ALLOCATION = "/app/academics/teacher-allocation";
    static final String ROUTE_CONFIGURATION = "/app/academics/timetable/configure";

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
        checkPatterns(data, checks);
        checkUnits(data, checks);
        checkAssignments(data, checks);
        checkFixedActivities(data, checks);
        checkSectionsAndSubjects(data, checks);
        checkUnitCapacity(data, checks);
        checkTeacherAllocations(data, checks);
        checkTeacherWorkload(data, checks);
        checkChoiceGroups(data, checks);
        checkRunningGeneration(data, checks);

        Set<Long> teacherIds = new HashSet<>();
        int requirementCount = 0;
        Set<Long> countedChoiceMappings = new HashSet<>();
        for (Lesson lesson : data.getLessons()) {
            teacherIds.add(lesson.teacher().getStaffId());
            AcademicGroupChoiceMapping choiceMapping = lesson.mapping().getAcademicGroupChoiceMapping();
            if (choiceMapping == null) {
                requirementCount += lesson.weeklyPeriods();
            } else if (countedChoiceMappings.add(choiceMapping.getAcademicGroupChoiceMappingId())) {
                requirementCount += choiceMapping.getWeeklyPeriods();
            }
        }
        int schedulableMappings = data.getMappings().size() - data.getZeroPeriodMappings().size();
        ReadinessSummary summary = new ReadinessSummary(
                data.getUnits().size(),
                schedulableMappings,
                requirementCount,
                teacherIds.size(),
                data.getResourceCount(),
                data.weeklySlotCount(),
                data.getMissingTeachers().size());

        return buildResponse(checks, data, summary);
    }

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

    private void checkPatterns(TimetablePlanningData data, List<ReadinessCheck> checks) {
        List<SchedulePattern> active = data.getPatterns().stream().filter(SchedulePattern::isActive).toList();
        if (active.isEmpty()) {
            checks.add(fail("SCHEDULE_PATTERNS_PRESENT", "Schedule patterns",
                    "No active schedule pattern exists. Create a reusable daily timeline first.", null,
                    "Create pattern", ROUTE_CONFIGURATION));
            return;
        }
        int withTeaching = 0;
        List<String> problems = new ArrayList<>();
        for (SchedulePattern pattern : active) {
            List<TimetablePeriod> slots = data.getSlotsByPattern()
                    .getOrDefault(pattern.getSchedulePatternId(), List.of());
            long teaching = slots.stream().filter(s -> s.getSlotKind() == TimetableSlotKind.TEACHING).count();
            if (teaching == 0) {
                problems.add("'" + pattern.getName() + "' has no teaching slots");
            } else {
                withTeaching++;
            }
            for (int i = 1; i < slots.size(); i++) {
                TimetablePeriod prev = slots.get(i - 1);
                TimetablePeriod cur = slots.get(i);
                if (cur.getStartTime().isBefore(prev.getEndTime())) {
                    problems.add("'" + pattern.getName() + "': " + prev.getName()
                            + " overlaps " + cur.getName());
                }
            }
        }
        if (withTeaching == 0) {
            checks.add(fail("SCHEDULE_PATTERNS_PRESENT", "Schedule patterns",
                    "No active pattern has a teaching slot.", problems, "Edit patterns", ROUTE_CONFIGURATION));
        } else if (!problems.isEmpty()) {
            checks.add(fail("SCHEDULE_PATTERNS_VALID", "Schedule patterns",
                    problems.size() + " schedule pattern issue(s) found.", problems,
                    "Fix patterns", ROUTE_CONFIGURATION));
        } else {
            checks.add(pass("SCHEDULE_PATTERNS_PRESENT", "Schedule patterns",
                    withTeaching + " active pattern(s) are ready"));
        }
    }

    private void checkUnits(TimetablePlanningData data, List<ReadinessCheck> checks) {
        if (data.getUnits().isEmpty()) {
            checks.add(fail("ACTIVE_SECTIONS_PRESENT", "Academic Groups & sections",
                    "No active academic groups exist for this academic year.", null,
                    "Open Academic Groups", ROUTE_CLASSES));
        } else {
            long withSections = data.getUnits().stream().filter(u -> u.section() != null).count();
            long groupOnly = data.getUnits().size() - withSections;
            checks.add(pass("ACTIVE_SECTIONS_PRESENT", "Academic Groups & sections",
                    withSections + " section unit(s) and " + groupOnly + " whole-group unit(s)"));
        }
    }

    private void checkAssignments(TimetablePlanningData data, List<ReadinessCheck> checks) {
        List<String> missing = new ArrayList<>();
        List<String> overlaps = new ArrayList<>();
        for (Unit unit : data.getUnits()) {
            if (!data.hasSchedule(unit.unitId())) {
                missing.add(unit.label() + " has no schedule pattern assigned on any day");
                continue;
            }
            for (DayOfWeek day : DayOfWeek.values()) {
                List<TimetablePeriod> slots = data.getScheduleByUnit()
                        .getOrDefault(unit.unitId(), Map.of()).getOrDefault(day, List.of());
                for (int i = 0; i < slots.size(); i++) {
                    for (int j = i + 1; j < slots.size(); j++) {
                        TimetablePeriod a = slots.get(i);
                        TimetablePeriod b = slots.get(j);
                        if (a.getStartTime() != null && b.getStartTime() != null
                                && a.getStartTime().isBefore(b.getEndTime())
                                && b.getStartTime().isBefore(a.getEndTime())) {
                            overlaps.add(unit.label() + " on " + day + ": '" + a.getName()
                                    + "' overlaps '" + b.getName() + "'");
                        }
                    }
                }
            }
        }
        checks.add(missing.isEmpty()
                ? pass("GROUP_SCHEDULES_ASSIGNED", "Group schedules",
                "Every scheduling unit has an active schedule assignment")
                : fail("GROUP_SCHEDULES_ASSIGNED", "Group schedules",
                missing.size() + " group/section unit(s) have no schedule pattern assigned.", missing,
                "Assign schedules", ROUTE_CONFIGURATION));
        checks.add(overlaps.isEmpty()
                ? pass("GROUP_SCHEDULES_NO_OVERLAP", "Schedule conflicts",
                "Assigned patterns do not overlap on the same day")
                : fail("GROUP_SCHEDULES_NO_OVERLAP", "Schedule conflicts",
                overlaps.size() + " overlapping schedule assignment(s) found.", overlaps,
                "Fix assignments", ROUTE_CONFIGURATION));
    }

    private void checkFixedActivities(TimetablePlanningData data, List<ReadinessCheck> checks) {
        List<String> problems = new ArrayList<>();
        int active = 0;
        for (FixedActivity activity : data.getFixedActivities()) {
            if (!activity.isActive()) continue;
            active++;
            String label = activity.getSubject() != null ? activity.getSubject().getName() : activity.getName();
            if (activity.getSubject() == null) {
                problems.add(label + " has no activity subject selected");
            }
            if (activity.getDays() == null || activity.getDays().isEmpty()) {
                problems.add(label + " has no day selected");
            }
            if (!Boolean.TRUE.equals(activity.getAppliesToAllGroups())
                    && (activity.getGroups() == null || activity.getGroups().isEmpty())) {
                problems.add(label + " has no academic group or section selected");
            }
            if (activity.getPeriods() == null || activity.getPeriods().isEmpty()) {
                // Legacy rows keep a time window instead of resolved periods.
                if (activity.getStartTime() == null || activity.getEndTime() == null
                        || !activity.getStartTime().isBefore(activity.getEndTime())) {
                    problems.add(label + " has no configured period or valid time");
                }
            } else {
                boolean unresolved = activity.getPeriods().stream()
                        .anyMatch(period -> period.getTimetablePeriod() == null);
                if (unresolved) {
                    problems.add(label + " has a period that is no longer configured");
                }
            }
            if (activity.getEffectiveFrom() != null && activity.getEffectiveTo() != null
                    && activity.getEffectiveTo().isBefore(activity.getEffectiveFrom())) {
                problems.add(label + " has an invalid effective date range");
            }
            if (activity.getSubject() != null
                    && isAcademicSubject(activity.getSubject().getSubjectType())) {
                problems.addAll(academicFixedActivityProblems(data, activity, label));
            }
        }
        if (active == 0) {
            checks.add(pass("FIXED_ACTIVITIES_VALID", "Fixed activities",
                    "No fixed activities configured (optional)"));
        } else if (!problems.isEmpty()) {
            checks.add(fail("FIXED_ACTIVITIES_VALID", "Fixed activities",
                    problems.size() + " fixed activity issue(s) found.", problems,
                    "Fix activities", ROUTE_CONFIGURATION));
        } else {
            checks.add(pass("FIXED_ACTIVITIES_VALID", "Fixed activities",
                    active + " fixed activity(ies) will be reserved first"));
        }
    }

    /**
     * An academic fixed lesson consumes part of the subject's mapped weekly frequency, so it
     * needs an active mapping, a resolved teacher and enough weekly periods to cover every day.
     */
    private List<String> academicFixedActivityProblems(TimetablePlanningData data, FixedActivity activity,
                                                       String label) {
        List<String> problems = new ArrayList<>();
        Long subjectId = activity.getSubject().getSubjectId();
        if (activity.getStaff() == null) {
            problems.add(label + " has no teacher allocated for the fixed academic lesson");
        }
        for (Unit unit : data.getUnits()) {
            if (!data.fixedActivitiesFor(unit).contains(activity)) continue;
            AcademicGroupSubjectMapping mapping = data.getMappings().stream()
                    .filter(candidate -> candidate.getAcademicGroup().getAcademicGroupId()
                            .equals(unit.academicGroupId()))
                    .filter(candidate -> candidate.getSubject().getSubjectId().equals(subjectId))
                    .filter(candidate -> Boolean.TRUE.equals(candidate.getActive()))
                    .filter(candidate -> candidate.getWeeklyPeriods() != null
                            && candidate.getWeeklyPeriods() > 0)
                    .findFirst().orElse(null);
            if (mapping == null) {
                problems.add(label + " is not mapped with weekly periods for " + unit.label());
                continue;
            }
            int fixedSessions = data.fixedActivitiesFor(unit).stream()
                    .filter(FixedActivity::isActive)
                    .filter(other -> other.getSubject() != null
                            && subjectId.equals(other.getSubject().getSubjectId()))
                    .mapToInt(other -> other.getDays() == null ? 0 : other.getDays().size())
                    .sum();
            if (fixedSessions > mapping.getWeeklyPeriods()) {
                problems.add(label + " reserves " + fixedSessions + " period(s)/week for " + unit.label()
                        + " but the weekly workload is " + mapping.getWeeklyPeriods());
            }
        }
        return problems;
    }

    private static boolean isAcademicSubject(SubjectType type) {
        return type == SubjectType.ACADEMIC_SUBJECT || type == SubjectType.PRACTICAL_LAB;
    }

    private void checkSectionsAndSubjects(TimetablePlanningData data, List<ReadinessCheck> checks) {
        int schedulable = data.getMappings().size() - data.getZeroPeriodMappings().size();
        if (schedulable == 0) {
            checks.add(fail("SUBJECT_MAPPINGS_PRESENT", "Curriculum mapping",
                    "No subjects with weekly periods are mapped to academic groups.", null,
                    "Map subjects", ROUTE_SUBJECTS));
        } else {
            Set<Long> groupsWithSubjects = new HashSet<>();
            for (AcademicGroupSubjectMapping m : data.getMappings()) {
                if (m.getWeeklyPeriods() != null && m.getWeeklyPeriods() > 0) {
                    groupsWithSubjects.add(m.getAcademicGroup().getAcademicGroupId());
                }
            }
            List<String> missing = data.getUnits().stream()
                    .filter(u -> !groupsWithSubjects.contains(u.academicGroupId()))
                    .map(u -> u.label() + " has no subjects mapped")
                    .toList();
            if (missing.isEmpty()) {
                checks.add(pass("SUBJECT_MAPPINGS_PRESENT", "Curriculum mapping",
                        schedulable + " academic group subject mapping(s) with weekly periods"));
            } else {
                checks.add(fail("SUBJECT_MAPPINGS_PRESENT", "Curriculum mapping",
                        missing.size() + " group/section unit(s) have no curriculum mapping.", missing,
                        "Map subjects", ROUTE_SUBJECTS));
            }
        }

        if (!data.getZeroPeriodMappings().isEmpty()) {
            List<String> details = data.getZeroPeriodMappings().stream()
                    .map(m -> m.getAcademicGroup().getName() + " • " + m.getSubject().getName()
                            + " has 0 weekly periods")
                    .toList();
            checks.add(fail("WEEKLY_PERIODS_POSITIVE", "Weekly periods",
                    details.size() + " curriculum mapping(s) have no valid weekly frequency.", details,
                    "Set weekly periods", ROUTE_SUBJECTS));
        } else {
            checks.add(pass("WEEKLY_PERIODS_POSITIVE", "Weekly periods",
                    "All mapped subjects have a weekly frequency"));
        }
    }

    private void checkUnitCapacity(TimetablePlanningData data, List<ReadinessCheck> checks) {
        Map<Long, Integer> requiredByGroup = new HashMap<>();
        Map<Long, Set<Long>> countedChoiceMappings = new HashMap<>();
        for (AcademicGroupSubjectMapping m : data.getMappings()) {
            if (m.getWeeklyPeriods() != null && m.getWeeklyPeriods() > 0) {
                AcademicGroupChoiceMapping choiceMapping = m.getAcademicGroupChoiceMapping();
                if (choiceMapping != null) {
                    if (!countedChoiceMappings.computeIfAbsent(m.getAcademicGroup().getAcademicGroupId(),
                            ignored -> new HashSet<>()).add(choiceMapping.getAcademicGroupChoiceMappingId())) {
                        continue;
                    }
                    requiredByGroup.merge(m.getAcademicGroup().getAcademicGroupId(),
                            (int) choiceMapping.getWeeklyPeriods(), Integer::sum);
                    continue;
                }
                requiredByGroup.merge(m.getAcademicGroup().getAcademicGroupId(),
                        (int) m.getWeeklyPeriods(), Integer::sum);
            }
        }
        List<String> over = new ArrayList<>();
        for (Unit unit : data.getUnits()) {
            int required = requiredByGroup.getOrDefault(unit.academicGroupId(), 0);
            int slots = data.getScheduleByUnit().getOrDefault(unit.unitId(), Map.of()).values().stream()
                    .mapToInt(list -> (int) list.stream()
                            .filter(s -> s.getSlotKind() == TimetableSlotKind.TEACHING).count())
                    .sum();
            if (slots == 0) continue;
            if (required > slots) {
                over.add(unit.label() + " needs " + required + " periods/week but its schedule has only "
                        + slots + " teaching slots");
            }
        }
        checks.add(over.isEmpty()
                ? pass("SECTION_CAPACITY", "Weekly capacity",
                "Every group/section fits within its assigned weekly slots")
                : fail("SECTION_CAPACITY", "Weekly capacity",
                over.size() + " unit(s) need more periods than their schedule provides.", over,
                "Adjust schedules", ROUTE_CONFIGURATION));
    }

    private void checkTeacherAllocations(TimetablePlanningData data, List<ReadinessCheck> checks) {
        List<MissingTeacher> missing = data.getMissingTeachers();
        if (missing.isEmpty()) {
            if (!data.getLessons().isEmpty()) {
                checks.add(pass("TEACHER_ALLOCATION_COMPLETE", "Teacher allocation",
                        "Every mapped subject has an active teacher"));
            } else {
                checks.add(fail("TEACHER_ALLOCATION_COMPLETE", "Teacher allocation",
                        "No subject has an active teacher assigned yet.", List.of(),
                        "Assign teachers", ROUTE_TEACHER_ALLOCATION));
            }
            return;
        }
        List<String> details = missing.stream()
                .map(m -> m.unit().label() + " • " + m.mapping().getSubject().getName()
                        + " – " + m.reason().getLabel()
                        + (m.teacherName() != null ? " (" + m.teacherName() + ")" : "")
                        + ", " + m.weeklyPeriods() + " period(s)")
                .toList();
        checks.add(fail("TEACHER_ALLOCATION_COMPLETE", "Teacher allocation",
                missing.size() + " curriculum mapping(s) have no active teacher.", details,
                "Assign teachers", ROUTE_TEACHER_ALLOCATION));
    }

    private void checkTeacherWorkload(TimetablePlanningData data, List<ReadinessCheck> checks) {
        TimetableConfiguration config = data.getConfiguration();
        int maxAllowed = config == null || config.getMaxTeacherWeeklyPeriods() == null
                ? 30 : config.getMaxTeacherWeeklyPeriods();

        Map<Long, Integer> load = new LinkedHashMap<>();
        Map<Long, Staff> staffById = new HashMap<>();
        for (Lesson lesson : data.getLessons()) {
            load.merge(lesson.teacher().getStaffId(), lesson.weeklyPeriods(), Integer::sum);
            staffById.put(lesson.teacher().getStaffId(), lesson.teacher());
        }
        // Academic fixed lessons are already included in the lesson weekly periods. Only
        // activity-type fixed staff add load that is not otherwise counted.
        for (FixedActivity activity : data.getFixedActivities()) {
            if (!activity.isActive() || activity.getStaff() == null || activity.getSubject() == null) continue;
            if (isAcademicSubject(activity.getSubject().getSubjectType())) continue;
            int days = activity.getDays() == null ? 0 : activity.getDays().size();
            if (days == 0) continue;
            long units = data.getUnits().stream()
                    .filter(unit -> data.fixedActivitiesFor(unit).contains(activity))
                    .count();
            if (units == 0) continue;
            Long staffId = activity.getStaff().getStaffId();
            load.merge(staffId, (int) (days * units), Integer::sum);
            staffById.putIfAbsent(staffId, activity.getStaff());
        }
        if (load.isEmpty()) return;
        List<String> over = new ArrayList<>();
        load.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .forEach(e -> {
                    if (e.getValue() > maxAllowed) {
                        over.add(TimetablePlanningData.staffName(staffById.get(e.getKey()))
                                + " – " + e.getValue() + " periods/week (limit " + maxAllowed + ")");
                    }
                });
        checks.add(over.isEmpty()
                ? pass("TEACHER_WORKLOAD_WITHIN_LIMIT", "Teacher workload",
                load.size() + " teacher(s), all within " + maxAllowed + " periods/week")
                : fail("TEACHER_WORKLOAD_WITHIN_LIMIT", "Teacher workload",
                over.size() + " teacher(s) are over their weekly limit.", over,
                "Rebalance allocation", ROUTE_TEACHER_ALLOCATION));
    }

    private void checkChoiceGroups(TimetablePlanningData data, List<ReadinessCheck> checks) {
        Map<Long, List<AcademicGroupSubjectMapping>> optionalByGroup = new HashMap<>();
        Map<String, List<AcademicGroupSubjectMapping>> choiceGroups = new HashMap<>();
        for (AcademicGroupSubjectMapping m : data.getMappings()) {
            if (m.getRequirement() != SubjectRequirement.OPTIONAL) continue;
            AcademicGroupChoiceMapping normalized = m.getAcademicGroupChoiceMapping();
            if (normalized != null && normalized.isActive()) {
                choiceGroups.computeIfAbsent("mapping|" + normalized.getAcademicGroupChoiceMappingId(),
                        ignored -> new ArrayList<>()).add(m);
            } else {
                optionalByGroup.computeIfAbsent(m.getAcademicGroup().getAcademicGroupId(),
                        k -> new ArrayList<>()).add(m);
                if (m.getChoiceGroup() != null && !m.getChoiceGroup().isBlank()) {
                    choiceGroups.computeIfAbsent(m.getAcademicGroup().getAcademicGroupId()
                            + "|" + m.getChoiceGroup().trim().toLowerCase(Locale.ROOT),
                            k -> new ArrayList<>()).add(m);
                }
            }
        }
        List<String> configured = choiceGroups.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(e -> {
                    AcademicGroupSubjectMapping first = e.getValue().get(0);
                    String groupName = first.getAcademicGroupChoiceMapping() == null
                            ? first.getChoiceGroup() : first.getAcademicGroupChoiceMapping().getChoiceGroup().getName();
                    return first.getAcademicGroup().getName() + " / " + groupName + ": "
                            + e.getValue().stream().map(m -> m.getSubject().getName())
                            .sorted().reduce((a, b) -> a + ", " + b).orElse("");
                }).sorted().toList();
        List<String> groups = optionalByGroup.entrySet().stream()
                .filter(e -> e.getValue().size() > 1)
                .map(e -> e.getValue().get(0).getAcademicGroup().getName() + ": "
                        + e.getValue().stream().map(m -> m.getSubject().getName())
                        .sorted().reduce((a, b) -> a + ", " + b).orElse(""))
                .toList();
        if (!configured.isEmpty()) {
            checks.add(warn("CHOICE_GROUPS_CONFIGURED", "Choice groups",
                    "Choice groups are configured. Group-level timetables schedule the published "
                            + "alternative periods; learner-level subject selection is required to choose "
                            + "one alternative per student.", configured, "Review mapping", ROUTE_SUBJECTS));
        } else if (!groups.isEmpty()) {
            checks.add(warn("CHOICE_GROUPS_CONFIGURED", "Choice groups",
                    "Competing optional subjects have no choice group. Add a choice group in Subject "
                            + "Mapping so alternatives are identified, or their periods will all be scheduled.",
                    groups, "Review mapping", ROUTE_SUBJECTS));
        } else {
            checks.add(pass("CHOICE_GROUPS_CONFIGURED", "Choice groups",
                    "No competing optional subjects require a choice-group rule"));
        }
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

    private TimetableReadinessResponse buildResponse(List<ReadinessCheck> checks,
                                                     TimetablePlanningData data,
                                                     ReadinessSummary summary) {
        List<ReadinessCheck> blocking = checks.stream()
                .filter(A -> A.getSeverity() == ReadinessSeverity.BLOCKING && A.getStatus() == ReadinessStatus.FAILED)
                .toList();
        List<ReadinessCheck> warnings = checks.stream()
                .filter(A -> A.getStatus() == ReadinessStatus.WARNING)
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

    private static LocalTime patternStart(TimetablePlanningData data, GroupScheduleAssignment assignment) {
        return data.getSlotsByPattern()
                .getOrDefault(assignment.getSchedulePattern().getSchedulePatternId(), List.of())
                .stream().map(TimetablePeriod::getStartTime).min(LocalTime::compareTo).orElse(null);
    }

    private static LocalTime patternEnd(TimetablePlanningData data, GroupScheduleAssignment assignment) {
        return data.getSlotsByPattern()
                .getOrDefault(assignment.getSchedulePattern().getSchedulePatternId(), List.of())
                .stream().map(TimetablePeriod::getEndTime).max(LocalTime::compareTo).orElse(null);
    }
}
