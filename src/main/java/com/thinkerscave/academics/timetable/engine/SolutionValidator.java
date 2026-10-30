package com.thinkerscave.academics.timetable.engine;

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
        validateChoiceGroupAlignment(solution, requirements, result);
        validateWorkload(solution, source, reqMap, result);
        validatePreferences(solution, reqMap, source, result);

        log.info("Validation: {} blocking, {} warnings",
                result.getBlockingConflicts().size(), result.getWarnings().size());
        return result;
    }

    private void validateCompleteness(SchedulingSolution solution, ValidationResult result) {
        Map<String, List<SchedulingRequirement>> grouped = new LinkedHashMap<>();
        for (SchedulingRequirement unplaced : solution.getUnplacedRequirements()) {
            grouped.computeIfAbsent(unplaced.getUnitId() + ":" + unplaced.getAcademicGroupSubjectMappingId(),
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
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        days.addAll(source.getWorkingDays());

        for (Placement p : solution.getPlacements()) {
            if (p.getEntryType() == com.thinkerscave.academics.enums.TimetableEntryType.ACTIVITY) continue;
            boolean allowed = source.isSlotAllowed(p.getUnitId(), p.getDayOfWeek(), p.getPeriodId());
            if (!allowed || !days.contains(p.getDayOfWeek())) {
                result.addBlocking(new ValidationConflict(
                        TimetableConflictType.PERIOD_CAPACITY_CONFLICT,
                        label(p, reqMap) + " is placed outside the group's assigned schedule",
                        p.getSectionId(), p.getTeacherAllocationId(), null, p.getDayOfWeek(), p.getPeriodId()));
            }
        }
    }

    private void validateUniqueness(SchedulingSolution solution, Map<String, SchedulingRequirement> reqMap,
                                    ValidationResult result) {
        Map<String, List<Placement>> unitSlots = new HashMap<>();
        Map<String, List<Placement>> teacherSlots = new HashMap<>();
        Map<String, List<Placement>> resourceSlots = new HashMap<>();

        for (Placement p : solution.getPlacements()) {
            String unitKey = p.getUnitId() + ":" + p.getDayOfWeek();
            if (overlapsUnit(unitSlots.computeIfAbsent(unitKey, k -> new ArrayList<>()), p)) {
                result.addBlocking(new ValidationConflict(
                        TimetableConflictType.SECTION_CONFLICT,
                        "Academic group clash: " + label(p, reqMap) + " overlaps another lesson on " + slotLabel(p),
                        p.getSectionId(), p.getTeacherAllocationId(), null, p.getDayOfWeek(), p.getPeriodId()));
            }
            unitSlots.get(unitKey).add(p);

            if (p.getStaffId() != null) {
                String teacherKey = p.getStaffId() + ":" + p.getDayOfWeek();
                List<Placement> sameTeacher = teacherSlots.computeIfAbsent(teacherKey, k -> new ArrayList<>());
                boolean clash = sameTeacher.stream().anyMatch(existing ->
                        existing.startMinute() < p.endMinute() && p.startMinute() < existing.endMinute()
                                && !sameChoiceAlternativeSlot(existing, p)
                                && !sameWholeSchoolActivity(existing, p));
                if (clash) {
                    result.addBlocking(new ValidationConflict(
                            TimetableConflictType.TEACHER_CONFLICT,
                            "Teacher clash: " + teacherName(p, reqMap) + " is booked for " + label(p, reqMap)
                                    + " in overlapping time on " + slotLabel(p),
                            p.getSectionId(), p.getTeacherAllocationId(), null, p.getDayOfWeek(), p.getPeriodId()));
                }
                sameTeacher.add(p);
            }

            if (p.getResourceId() != null) {
                String resourceKey = p.getResourceId() + ":" + p.getDayOfWeek();
                if (overlaps(resourceSlots.computeIfAbsent(resourceKey, k -> new ArrayList<>()), p)) {
                    result.addBlocking(new ValidationConflict(
                            TimetableConflictType.ROOM_CONFLICT,
                            "Room clash: " + label(p, reqMap) + " overlaps another lesson in the same room on "
                                    + slotLabel(p),
                            p.getSectionId(), null, p.getResourceId(), p.getDayOfWeek(), p.getPeriodId()));
                }
                resourceSlots.get(resourceKey).add(p);
            }
        }
    }

    private static boolean overlaps(List<Placement> list, Placement candidate) {
        return list.stream().anyMatch(p -> p.startMinute() < candidate.endMinute()
                && candidate.startMinute() < p.endMinute());
    }

    private static boolean overlapsUnit(List<Placement> list, Placement candidate) {
        return list.stream().anyMatch(existing ->
                existing.startMinute() < candidate.endMinute()
                        && candidate.startMinute() < existing.endMinute()
                        && !sameChoiceAlternativeSlot(existing, candidate));
    }

    private static boolean sameChoiceAlternativeSlot(Placement first, Placement second) {
        return first.getChoiceMappingId() != null
                && Objects.equals(first.getChoiceMappingId(), second.getChoiceMappingId())
                && first.getPeriodIndex() == second.getPeriodIndex()
                && first.getDayOfWeek() == second.getDayOfWeek()
                && Objects.equals(first.getPeriodId(), second.getPeriodId())
                && !Objects.equals(first.getAcademicGroupSubjectMappingId(),
                second.getAcademicGroupSubjectMappingId())
                && !Objects.equals(first.getSubjectId(), second.getSubjectId());
    }

    /** The same whole-school fixed activity booked for several units at the same time is one event. */
    private static boolean sameWholeSchoolActivity(Placement first, Placement second) {
        return first.getFixedActivityId() != null
                && Objects.equals(first.getFixedActivityId(), second.getFixedActivityId())
                && first.getDayOfWeek() == second.getDayOfWeek()
                && Objects.equals(first.getPeriodId(), second.getPeriodId());
    }

    private void validateChoiceGroupAlignment(SchedulingSolution solution,
                                              List<SchedulingRequirement> requirements,
                                              ValidationResult result) {
        Map<String, List<SchedulingRequirement>> alternativesBySlot = new LinkedHashMap<>();
        for (SchedulingRequirement requirement : requirements) {
            if (requirement.getChoiceMappingId() == null) continue;
            String key = requirement.getUnitId() + "|" + requirement.getChoiceMappingId()
                    + "|" + requirement.getPeriodIndex();
            alternativesBySlot.computeIfAbsent(key, ignored -> new ArrayList<>()).add(requirement);
        }
        Map<String, Placement> placementsByRequirement = solution.getPlacements().stream()
                .collect(java.util.stream.Collectors.toMap(Placement::getRequirementId, p -> p, (first, ignored) -> first));
        for (List<SchedulingRequirement> alternatives : alternativesBySlot.values()) {
            if (alternatives.stream().map(SchedulingRequirement::getAcademicGroupSubjectMappingId)
                    .distinct().count() < 2) continue;
            List<Placement> placed = alternatives.stream()
                    .map(SchedulingRequirement::getRequirementId)
                    .map(placementsByRequirement::get)
                    .filter(Objects::nonNull)
                    .toList();
            if (placed.size() < 2) continue;
            Placement first = placed.get(0);
            boolean aligned = placed.stream().allMatch(placement ->
                    placement.getDayOfWeek() == first.getDayOfWeek()
                            && Objects.equals(placement.getPeriodId(), first.getPeriodId()));
            if (!aligned) {
                SchedulingRequirement requirement = alternatives.get(0);
                result.addBlocking(new ValidationConflict(
                        TimetableConflictType.CHOICE_GROUP_CONFLICT,
                        "Alternative subjects for " + requirement.getSectionName()
                                + " are not scheduled in the same choice-group period.",
                        requirement.getSectionId(), requirement.getTeacherAllocationId(), null,
                        first.getDayOfWeek(), first.getPeriodId()));
            }
        }
    }

    private void validateWorkload(SchedulingSolution solution, SchedulingSourceData source,
                                  Map<String, SchedulingRequirement> reqMap, ValidationResult result) {
        Map<Long, Integer> weeklyLoad = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        Set<String> wholeSchoolSlots = new HashSet<>();
        for (Placement p : solution.getPlacements()) {
            if (p.getStaffId() == null) continue;
            // Count a whole-school activity once per teacher, not once per unit.
            if (p.getFixedActivityId() != null) {
                String slotKey = p.getStaffId() + "|" + p.getDayOfWeek() + "|" + p.getPeriodId();
                if (!wholeSchoolSlots.add(slotKey)) continue;
            }
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
        Map<String, int[]> missesByLesson = new LinkedHashMap<>();
        Map<String, SchedulingRequirement> lessonReq = new HashMap<>();

        for (Placement p : solution.getPlacements()) {
            SchedulingRequirement req = reqMap.get(p.getRequirementId());
            if (req == null || req.getPreference() == null || req.getPreference() == SubjectTimetablePreference.ANY) {
                continue;
            }
            boolean firstHalf = p.getTeachingIndex() < source.halfIndex(p.getDayOfWeek());
            boolean missed = req.getPreference() == SubjectTimetablePreference.FIRST_HALF ? !firstHalf : firstHalf;
            if (missed) {
                String key = req.getUnitId() + ":" + req.getAcademicGroupSubjectMappingId();
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
        return day.charAt(0) + day.substring(1).toLowerCase() + " "
                + (p.getStartTime() == null ? "period " + p.getPeriodNumber() : p.getStartTime() + "–" + p.getEndTime());
    }

    @lombok.Getter
    public static class ValidationResult {
        private final List<ValidationConflict> blockingConflicts = new ArrayList<>();
        private final List<ValidationConflict> warnings = new ArrayList<>();

        public void addBlocking(ValidationConflict A) { blockingConflicts.add(A); }
        public void addWarning(ValidationConflict A) { warnings.add(A); }
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
