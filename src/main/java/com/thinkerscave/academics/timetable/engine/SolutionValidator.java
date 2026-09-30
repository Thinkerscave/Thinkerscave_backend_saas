package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.entity.TimetablePeriod;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import com.thinkerscave.academics.enums.TimetableConflictType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Independently re-checks a finished schedule. Hard rule violations become blocking
 * conflicts; unmet preferences become warnings.
 */
@Component
public class SolutionValidator {

    private static final Logger log = LoggerFactory.getLogger(SolutionValidator.class);

    public ValidationResult validate(SchedulingSolution solution,
                                     List<SchedulingRequirement> requirements,
                                     SchedulingSourceData source) {
        ValidationResult result = new ValidationResult();

        Map<String, SchedulingRequirement> reqMap = new HashMap<>();
        for (SchedulingRequirement r : requirements) {
            reqMap.put(r.getRequirementId(), r);
        }

        validateCompleteness(solution, result);
        validateSlotsExist(solution, source, reqMap, result);
        validateUniqueness(solution, reqMap, result);
        validateWorkload(solution, source, reqMap, result);
        validatePreferences(solution, reqMap, source, result);

        log.info("Validation: {} blocking, {} warnings",
                result.getBlockingConflicts().size(), result.getWarnings().size());
        return result;
    }

    private void validateCompleteness(SchedulingSolution solution, ValidationResult result) {
        Map<String, List<SchedulingRequirement>> grouped = new LinkedHashMap<>();
        for (SchedulingRequirement unplaced : solution.getUnplacedRequirements()) {
            grouped.computeIfAbsent(unplaced.getSectionId() + ":" + unplaced.getClassSubjectMappingId(),
                    k -> new ArrayList<>()).add(unplaced);
        }
        for (List<SchedulingRequirement> group : grouped.values()) {
            SchedulingRequirement r = group.get(0);
            result.addBlocking(new ValidationConflict(
                    TimetableConflictType.PERIOD_CAPACITY_CONFLICT,
                    "Could not fit " + group.size() + " of " + r.getWeeklyPeriods() + " " + r.getSubjectName()
                            + " period(s) for " + r.getSectionName() + " (teacher " + r.getPrimaryStaffName()
                            + ") without a clash. Reduce weekly periods, spread the teacher's load, or add periods.",
                    r.getSectionId(), r.getTeacherAllocationId(), null, null, null));
        }
    }

    private void validateSlotsExist(SchedulingSolution solution, SchedulingSourceData source,
                                    Map<String, SchedulingRequirement> reqMap, ValidationResult result) {
        Set<Long> teachingPeriodIds = new HashSet<>();
        for (TimetablePeriod p : source.getTeachingPeriods()) teachingPeriodIds.add(p.getTimetablePeriodId());
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        days.addAll(source.getWorkingDays());

        for (Placement p : solution.getPlacements()) {
            if (!teachingPeriodIds.contains(p.getPeriodId()) || !days.contains(p.getDayOfWeek())) {
                result.addBlocking(new ValidationConflict(
                        TimetableConflictType.PERIOD_CAPACITY_CONFLICT,
                        label(p, reqMap) + " is placed outside the configured teaching periods/working days",
                        p.getSectionId(), p.getTeacherAllocationId(), null, p.getDayOfWeek(), p.getPeriodId()));
            }
        }
    }

    private void validateUniqueness(SchedulingSolution solution, Map<String, SchedulingRequirement> reqMap,
                                    ValidationResult result) {
        Map<String, Placement> sectionSlotMap = new HashMap<>();
        Map<String, Placement> teacherSlotMap = new HashMap<>();
        Map<String, Placement> resourceSlotMap = new HashMap<>();

        for (Placement p : solution.getPlacements()) {
            String sectionKey = p.getSectionId() + ":" + p.getDayOfWeek() + ":" + p.getPeriodId();
            Placement sectionClash = sectionSlotMap.putIfAbsent(sectionKey, p);
            if (sectionClash != null) {
                result.addBlocking(new ValidationConflict(
                        TimetableConflictType.SECTION_CONFLICT,
                        "Class clash: " + label(p, reqMap) + " and " + label(sectionClash, reqMap)
                                + " share " + slotLabel(p),
                        p.getSectionId(), p.getTeacherAllocationId(), null, p.getDayOfWeek(), p.getPeriodId()));
            }

            String teacherKey = p.getStaffId() + ":" + p.getDayOfWeek() + ":" + p.getPeriodId();
            Placement teacherClash = teacherSlotMap.putIfAbsent(teacherKey, p);
            if (teacherClash != null) {
                result.addBlocking(new ValidationConflict(
                        TimetableConflictType.TEACHER_CONFLICT,
                        "Teacher clash: " + teacherName(p, reqMap) + " is booked for " + label(p, reqMap)
                                + " and " + label(teacherClash, reqMap) + " on " + slotLabel(p),
                        p.getSectionId(), p.getTeacherAllocationId(), null, p.getDayOfWeek(), p.getPeriodId()));
            }

            if (p.getResourceId() != null) {
                String resourceKey = p.getResourceId() + ":" + p.getDayOfWeek() + ":" + p.getPeriodId();
                Placement roomClash = resourceSlotMap.putIfAbsent(resourceKey, p);
                if (roomClash != null) {
                    result.addBlocking(new ValidationConflict(
                            TimetableConflictType.ROOM_CONFLICT,
                            "Room clash: " + label(p, reqMap) + " and " + label(roomClash, reqMap)
                                    + " use the same room on " + slotLabel(p),
                            p.getSectionId(), null, p.getResourceId(), p.getDayOfWeek(), p.getPeriodId()));
                }
            }
        }
    }

    private void validateWorkload(SchedulingSolution solution, SchedulingSourceData source,
                                  Map<String, SchedulingRequirement> reqMap, ValidationResult result) {
        Map<Long, Integer> weeklyLoad = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        for (Placement p : solution.getPlacements()) {
            weeklyLoad.merge(p.getStaffId(), 1, Integer::sum);
            names.putIfAbsent(p.getStaffId(), teacherName(p, reqMap));
        }

        int max = source.getMaxTeacherWeeklyPeriods();
        for (Map.Entry<Long, Integer> entry : weeklyLoad.entrySet()) {
            if (entry.getValue() > max) {
                result.addBlocking(new ValidationConflict(
                        TimetableConflictType.WORKLOAD_CONFLICT,
                        names.get(entry.getKey()) + " is scheduled for " + entry.getValue()
                                + " periods/week, above the limit of " + max,
                        null, null, null, null, null));
            }
        }
    }

    private void validatePreferences(SchedulingSolution solution,
                                     Map<String, SchedulingRequirement> reqMap,
                                     SchedulingSourceData source,
                                     ValidationResult result) {
        int halfIndex = source.getHalfIndex();
        Map<String, int[]> missesByLesson = new LinkedHashMap<>();
        Map<String, SchedulingRequirement> lessonReq = new HashMap<>();

        for (Placement p : solution.getPlacements()) {
            SchedulingRequirement req = reqMap.get(p.getRequirementId());
            if (req == null || req.getPreference() == null || req.getPreference() == SubjectTimetablePreference.ANY) {
                continue;
            }
            boolean firstHalf = p.getTeachingIndex() < halfIndex;
            boolean missed = req.getPreference() == SubjectTimetablePreference.FIRST_HALF ? !firstHalf : firstHalf;
            if (missed) {
                String key = req.getSectionId() + ":" + req.getClassSubjectMappingId();
                missesByLesson.computeIfAbsent(key, k -> new int[1])[0]++;
                lessonReq.putIfAbsent(key, req);
            }
        }

        for (Map.Entry<String, int[]> e : missesByLesson.entrySet()) {
            SchedulingRequirement req = lessonReq.get(e.getKey());
            String half = req.getPreference() == SubjectTimetablePreference.FIRST_HALF ? "first" : "second";
            result.addWarning(new ValidationConflict(
                    TimetableConflictType.SUBJECT_PREFERENCE_CONFLICT,
                    req.getSubjectName() + " for " + req.getSectionName() + ": " + e.getValue()[0] + " of "
                            + req.getWeeklyPeriods() + " period(s) could not be placed in the preferred " + half
                            + " half of the day",
                    req.getSectionId(), req.getTeacherAllocationId(), null, null, null));
        }
    }

    private static String label(Placement p, Map<String, SchedulingRequirement> reqMap) {
        SchedulingRequirement r = reqMap.get(p.getRequirementId());
        return r == null ? p.getSubjectNameSnapshot() : r.getSubjectName() + " (" + r.getSectionName() + ")";
    }

    private static String teacherName(Placement p, Map<String, SchedulingRequirement> reqMap) {
        SchedulingRequirement r = reqMap.get(p.getRequirementId());
        return r == null ? "Teacher #" + p.getStaffId() : r.getPrimaryStaffName();
    }

    private static String slotLabel(Placement p) {
        String day = p.getDayOfWeek().name();
        return day.charAt(0) + day.substring(1).toLowerCase() + " period " + p.getPeriodNumber();
    }

    @lombok.Getter
    public static class ValidationResult {
        private final List<ValidationConflict> blockingConflicts = new ArrayList<>();
        private final List<ValidationConflict> warnings = new ArrayList<>();

        public void addBlocking(ValidationConflict c) { blockingConflicts.add(c); }
        public void addWarning(ValidationConflict c) { warnings.add(c); }
        public boolean hasBlockingIssues() { return !blockingConflicts.isEmpty(); }
    }

    @lombok.Getter
    @lombok.AllArgsConstructor
    public static class ValidationConflict {
        private final TimetableConflictType conflictType;
        private final String message;
        private final Long sectionId;
        private final Long teacherAllocationId;
        private final Long resourceId;
        private final DayOfWeek dayOfWeek;
        private final Long periodId;
    }
}
