package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import com.thinkerscave.academics.enums.TimetableConflictType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SolutionValidator {

    private static final Logger log = LoggerFactory.getLogger(SolutionValidator.class);

    public ValidationResult validate(SchedulingSolution solution,
                                     List<SchedulingRequirement> requirements,
                                     SchedulingSourceData source) {
        ValidationResult result = new ValidationResult();

        validateCompleteness(solution, requirements, result);
        validateUniqueness(solution, result);
        validateWorkload(solution, source, result);
        validatePreferences(solution, requirements, source, result);

        log.info("Validation: {} blocking, {} warnings",
                result.getBlockingConflicts().size(), result.getWarnings().size());
        return result;
    }

    private void validateCompleteness(SchedulingSolution solution,
                                      List<SchedulingRequirement> requirements,
                                      ValidationResult result) {
        if (!solution.getUnplacedRequirements().isEmpty()) {
            for (SchedulingRequirement unplaced : solution.getUnplacedRequirements()) {
                result.addBlocking(new ValidationConflict(
                        TimetableConflictType.SUBJECT_ALLOCATION_CONFLICT,
                        "Cannot schedule " + unplaced.getSubjectName()
                                + " in section " + unplaced.getSectionName()
                                + " (period " + (unplaced.getPeriodIndex() + 1)
                                + "/" + unplaced.getWeeklyPeriods() + ")",
                        unplaced.getSectionId(),
                        unplaced.getTeacherAllocationId(),
                        null, null, null));
            }
        }
    }

    private void validateUniqueness(SchedulingSolution solution, ValidationResult result) {
        Map<String, Placement> sectionSlotMap = new HashMap<>();
        Map<String, Placement> teacherSlotMap = new HashMap<>();
        Map<String, Placement> resourceSlotMap = new HashMap<>();

        for (Placement p : solution.getPlacements()) {
            String sectionKey = p.getSectionId() + ":" + p.getDayOfWeek() + ":" + p.getPeriodId();
            if (sectionSlotMap.containsKey(sectionKey)) {
                result.addBlocking(new ValidationConflict(
                        TimetableConflictType.SECTION_CONFLICT,
                        "Section slot conflict at " + p.getDayOfWeek() + " period " + p.getPeriodNumber(),
                        p.getSectionId(), null, null, p.getDayOfWeek(), p.getPeriodId()));
            }
            sectionSlotMap.put(sectionKey, p);

            String teacherKey = p.getStaffId() + ":" + p.getDayOfWeek() + ":" + p.getPeriodId();
            if (teacherSlotMap.containsKey(teacherKey)) {
                result.addBlocking(new ValidationConflict(
                        TimetableConflictType.TEACHER_CONFLICT,
                        "Teacher slot conflict at " + p.getDayOfWeek() + " period " + p.getPeriodNumber(),
                        p.getSectionId(), p.getTeacherAllocationId(), null, p.getDayOfWeek(), p.getPeriodId()));
            }
            teacherSlotMap.put(teacherKey, p);

            if (p.getResourceId() != null) {
                String resourceKey = p.getResourceId() + ":" + p.getDayOfWeek() + ":" + p.getPeriodId();
                if (resourceSlotMap.containsKey(resourceKey)) {
                    result.addBlocking(new ValidationConflict(
                            TimetableConflictType.ROOM_CONFLICT,
                            "Resource slot conflict at " + p.getDayOfWeek() + " period " + p.getPeriodNumber(),
                            p.getSectionId(), null, p.getResourceId(), p.getDayOfWeek(), p.getPeriodId()));
                }
                resourceSlotMap.put(resourceKey, p);
            }
        }
    }

    private void validateWorkload(SchedulingSolution solution, SchedulingSourceData source,
                                  ValidationResult result) {
        Map<Long, Integer> weeklyLoad = new HashMap<>();
        for (Placement p : solution.getPlacements()) {
            weeklyLoad.merge(p.getStaffId(), 1, Integer::sum);
        }

        int max = source.getMaxTeacherWeeklyPeriods();
        for (Map.Entry<Long, Integer> entry : weeklyLoad.entrySet()) {
            if (entry.getValue() > max) {
                result.addBlocking(new ValidationConflict(
                        TimetableConflictType.WORKLOAD_CONFLICT,
                        "Teacher (staff=" + entry.getKey() + ") workload " + entry.getValue()
                                + " exceeds max " + max,
                        null, null, null, null, null));
            }
        }
    }

    private void validatePreferences(SchedulingSolution solution,
                                     List<SchedulingRequirement> requirements,
                                     SchedulingSourceData source,
                                     ValidationResult result) {
        int halfIndex = source.getHalfIndex();
        Map<String, SchedulingRequirement> reqMap = new HashMap<>();
        for (SchedulingRequirement r : requirements) {
            reqMap.put(r.getRequirementId(), r);
        }

        for (Placement p : solution.getPlacements()) {
            SchedulingRequirement req = reqMap.get(p.getRequirementId());
            if (req == null) continue;

            SubjectTimetablePreference pref = req.getPreference();
            if (pref == SubjectTimetablePreference.FIRST_HALF && p.getPeriodNumber() > halfIndex) {
                result.addWarning(new ValidationConflict(
                        TimetableConflictType.SUBJECT_ALLOCATION_CONFLICT,
                        req.getSubjectName() + " in " + req.getSectionName()
                                + " prefers FIRST_HALF but placed in period " + p.getPeriodNumber(),
                        req.getSectionId(), req.getTeacherAllocationId(), null,
                        p.getDayOfWeek(), p.getPeriodId()));
            }
            if (pref == SubjectTimetablePreference.SECOND_HALF && p.getPeriodNumber() <= halfIndex) {
                result.addWarning(new ValidationConflict(
                        TimetableConflictType.SUBJECT_ALLOCATION_CONFLICT,
                        req.getSubjectName() + " in " + req.getSectionName()
                                + " prefers SECOND_HALF but placed in period " + p.getPeriodNumber(),
                        req.getSectionId(), req.getTeacherAllocationId(), null,
                        p.getDayOfWeek(), p.getPeriodId()));
            }
        }
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
