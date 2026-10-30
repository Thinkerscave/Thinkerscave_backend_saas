package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.entity.TimetablePeriod;
import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import com.thinkerscave.academics.enums.TimetableEntryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.*;

/**
 * Improves subject spread and first/second-half preferences by relocating lessons to empty
 * slots and swapping pairs of lessons within the same section. Every move is verified against
 * section, teacher and resource occupancy, so hard constraints never regress.
 */
@Component
public class SoftConstraintOptimizer {

    private static final Logger log = LoggerFactory.getLogger(SoftConstraintOptimizer.class);
    /** Enough passes to converge after the greedy scheduler; usually stops much earlier. */
    private static final int MAX_OPTIMIZATION_PASSES = 20;
    private static final long SHUFFLE_SEED = 20261030L;

    public SchedulingSolution optimize(SchedulingSolution solution,
                                       SchedulingSourceData source,
                                       GenerationProgress progress) {
        return optimize(solution, source, progress, Map.of());
    }

    public SchedulingSolution optimize(SchedulingSolution solution,
                                       SchedulingSourceData source,
                                       GenerationProgress progress,
                                       Map<String, SchedulingRequirement> requirementsById) {
        progress.advanceTo(GenerationPhase.OPTIMIZING, 72);

        Map<Long, List<Placement>> byUnit = new HashMap<>();
        for (Placement p : solution.getPlacements()) {
            byUnit.computeIfAbsent(p.getUnitId(), k -> new ArrayList<>()).add(p);
        }

        int improvementCount = 0;
        List<Long> unitIds = new ArrayList<>(byUnit.keySet());
        for (int pass = 0; pass < MAX_OPTIMIZATION_PASSES; pass++) {
            if (progress.isCancelRequested()) break;
            int passImprovements = 0;
            // Different orders reach different local optima; shuffling makes the search escape
            // the order imposed by the greedy scheduler while staying reproducible.
            Collections.shuffle(unitIds, new Random(SHUFFLE_SEED + pass));
            for (Long unitId : unitIds) {
                List<Placement> sectionPlacements = new ArrayList<>(byUnit.get(unitId));
                Collections.shuffle(sectionPlacements,
                        new Random(SHUFFLE_SEED + pass * 97L + unitId));
                passImprovements += runEmptySlotPass(
                        sectionPlacements, solution, source, requirementsById);
                passImprovements += runSwapPass(
                        sectionPlacements, solution, source, requirementsById);
            }
            improvementCount += passImprovements;
            if (passImprovements == 0) break;
        }

        progress.advanceTo(GenerationPhase.OPTIMIZING, 80);
        log.info("Optimization complete: {} moves applied, {} preference misses remain",
                improvementCount, countPreferenceMisses(solution, source, requirementsById));
        return solution;
    }

    /**
     * Moves a lesson into an unused slot of the same section when that improves the soft score.
     * This is what recovers free periods created by uneven weekly loads.
     */
    private int runEmptySlotPass(List<Placement> placements, SchedulingSolution solution,
                                 SchedulingSourceData source,
                                 Map<String, SchedulingRequirement> requirementsById) {
        int improvements = 0;
        for (Placement placement : placements) {
            if (isLocked(placement)) continue;
            double before = evaluate(placement, solution, source, requirementsById);
            boolean moved = false;
            for (DayOfWeek day : source.getWorkingDays()) {
                for (TimetablePeriod slot : source.slotsForUnitOnDay(placement.getUnitId(), day)) {
                    if (solution.isSectionOccupied(placement.getUnitId(), day,
                            slot.getTimetablePeriodId())) continue;
                    if (placement.getStaffId() != null
                            && solution.isTeacherOccupied(placement.getStaffId(), day,
                            slot.getStartTime(), slot.getEndTime())) continue;
                    if (placement.getResourceId() != null
                            && solution.isResourceOccupied(placement.getResourceId(), day,
                            slot.getStartTime(), slot.getEndTime())) continue;

                    PlacementSnapshot original = PlacementSnapshot.of(placement);
                    solution.unplace(placement);
                    applySlot(placement, day, slot, source);
                    solution.place(placement);
                    double after = evaluate(placement, solution, source, requirementsById);
                    if (after > before + 0.001) {
                        improvements++;
                        moved = true;
                        break;
                    }
                    solution.unplace(placement);
                    original.restore(placement);
                    solution.place(placement);
                }
                if (moved) break;
            }
        }
        return improvements;
    }

    private int runSwapPass(List<Placement> placements, SchedulingSolution solution,
                            SchedulingSourceData source, Map<String, SchedulingRequirement> requirementsById) {
        int improvements = 0;
        for (int a = 0; a < placements.size(); a++) {
            for (int b = a + 1; b < placements.size(); b++) {
                Placement pA = placements.get(a);
                Placement pB = placements.get(b);
                if (isLocked(pA) || isLocked(pB)) continue;
                if (pA.getAcademicGroupSubjectMappingId() == null
                        || Objects.equals(pA.getAcademicGroupSubjectMappingId(),
                        pB.getAcademicGroupSubjectMappingId())) continue;
                if (pA.getDayOfWeek() == pB.getDayOfWeek() && pA.getPeriodId().equals(pB.getPeriodId())) continue;

                double before = evaluate(pA, solution, source, requirementsById)
                        + evaluate(pB, solution, source, requirementsById);
                if (!canSwapSlots(pA, pB, solution, source)) continue;

                swapSlots(pA, pB, solution);
                double after = evaluate(pA, solution, source, requirementsById)
                        + evaluate(pB, solution, source, requirementsById);
                if (after > before + 0.001) {
                    improvements++;
                } else {
                    swapSlots(pA, pB, solution);
                }
            }
        }
        return improvements;
    }

    private boolean canSwapSlots(Placement pA, Placement pB, SchedulingSolution solution,
                                 SchedulingSourceData source) {
        if (!source.isSlotAllowed(pA.getUnitId(), pB.getDayOfWeek(), pB.getPeriodId())) return false;
        if (!source.isSlotAllowed(pB.getUnitId(), pA.getDayOfWeek(), pA.getPeriodId())) return false;
        solution.unplace(pA);
        solution.unplace(pB);

        boolean canPlaceAinB = !solution.isSectionOccupied(pA.getUnitId(), pB.getDayOfWeek(),
                pB.getStartTime(), pB.getEndTime())
                && (pA.getStaffId() == null || !solution.isTeacherOccupied(pA.getStaffId(), pB.getDayOfWeek(),
                pB.getStartTime(), pB.getEndTime()))
                && (pA.getResourceId() == null || !solution.isResourceOccupied(pA.getResourceId(), pB.getDayOfWeek(),
                pB.getStartTime(), pB.getEndTime()));

        boolean canPlaceBinA = !solution.isSectionOccupied(pB.getUnitId(), pA.getDayOfWeek(),
                pA.getStartTime(), pA.getEndTime())
                && (pB.getStaffId() == null || !solution.isTeacherOccupied(pB.getStaffId(), pA.getDayOfWeek(),
                pA.getStartTime(), pA.getEndTime()))
                && (pB.getResourceId() == null || !solution.isResourceOccupied(pB.getResourceId(), pA.getDayOfWeek(),
                pA.getStartTime(), pA.getEndTime()));

        solution.place(pA);
        solution.place(pB);

        return canPlaceAinB && canPlaceBinA;
    }

    private void swapSlots(Placement pA, Placement pB, SchedulingSolution solution) {
        solution.unplace(pA);
        solution.unplace(pB);

        DayOfWeek dayA = pA.getDayOfWeek();
        Long periodIdA = pA.getPeriodId();
        short periodNumA = pA.getPeriodNumber();
        int teachingIdxA = pA.getTeachingIndex();
        java.time.LocalTime startA = pA.getStartTime();
        java.time.LocalTime endA = pA.getEndTime();

        pA.setDayOfWeek(pB.getDayOfWeek());
        pA.setPeriodId(pB.getPeriodId());
        pA.setPeriodNumber(pB.getPeriodNumber());
        pA.setTeachingIndex(pB.getTeachingIndex());
        pA.setStartTime(pB.getStartTime());
        pA.setEndTime(pB.getEndTime());

        pB.setDayOfWeek(dayA);
        pB.setPeriodId(periodIdA);
        pB.setPeriodNumber(periodNumA);
        pB.setTeachingIndex(teachingIdxA);
        pB.setStartTime(startA);
        pB.setEndTime(endA);

        solution.place(pA);
        solution.place(pB);
    }

    private double evaluate(Placement p, SchedulingSolution solution, SchedulingSourceData source,
                            Map<String, SchedulingRequirement> requirementsById) {
        SchedulingRequirement req = requirementsById.get(p.getRequirementId());
        int weekly = req == null ? 1 : req.getWeeklyPeriods();
        int cap = CandidateScorer.dailyCap(weekly, source.workingDayCount());
        int sameDay = solution.countPlacementsForSubjectInDay(
                p.getUnitId(), p.getAcademicGroupSubjectMappingId(), p.getDayOfWeek());
        double score;
        if (sameDay > cap) {
            score = -120.0 * (sameDay - cap);
        } else {
            score = sameDay == 1 ? 30.0 : 5.0;
        }

        if (req != null && req.getPreference() != null && req.getPreference() != SubjectTimetablePreference.ANY) {
            boolean firstHalf = p.getTeachingIndex() < source.halfIndex(p.getDayOfWeek());
            boolean wantsFirst = req.getPreference() == SubjectTimetablePreference.FIRST_HALF;
            score += firstHalf == wantsFirst ? 20.0 : -20.0;
        }

        if (solution.hasConsecutiveSubject(p.getUnitId(), p.getAcademicGroupSubjectMappingId(),
                p.getDayOfWeek(), p.getPeriodNumber())) {
            score += weekly > source.workingDayCount() ? -5.0 : -20.0;
        }
        return score;
    }

    private static boolean isLocked(Placement placement) {
        return placement.getEntryType() == TimetableEntryType.ACTIVITY
                || placement.getChoiceMappingId() != null;
    }

    private static void applySlot(Placement placement, DayOfWeek day, TimetablePeriod slot,
                                  SchedulingSourceData source) {
        List<TimetablePeriod> daySlots = source.slotsForUnitOnDay(placement.getUnitId(), day);
        int teachingIndex = 0;
        for (int i = 0; i < daySlots.size(); i++) {
            if (Objects.equals(daySlots.get(i).getTimetablePeriodId(),
                    slot.getTimetablePeriodId())) {
                teachingIndex = i;
                break;
            }
        }
        placement.setDayOfWeek(day);
        placement.setPeriodId(slot.getTimetablePeriodId());
        placement.setPeriodNumber(slot.getPeriodNumber());
        placement.setTeachingIndex(teachingIndex);
        placement.setStartTime(slot.getStartTime());
        placement.setEndTime(slot.getEndTime());
    }

    private static int countPreferenceMisses(SchedulingSolution solution, SchedulingSourceData source,
                                             Map<String, SchedulingRequirement> requirementsById) {
        int misses = 0;
        for (Placement placement : solution.getPlacements()) {
            SchedulingRequirement req = requirementsById.get(placement.getRequirementId());
            if (req == null || req.getPreference() == null
                    || req.getPreference() == SubjectTimetablePreference.ANY) continue;
            boolean firstHalf = placement.getTeachingIndex() < source.halfIndex(placement.getDayOfWeek());
            boolean wantsFirst = req.getPreference() == SubjectTimetablePreference.FIRST_HALF;
            if (firstHalf != wantsFirst) misses++;
        }
        return misses;
    }

    private record PlacementSnapshot(DayOfWeek day, Long periodId, short periodNumber,
                                     int teachingIndex, LocalTime start, LocalTime end) {

        static PlacementSnapshot of(Placement placement) {
            return new PlacementSnapshot(placement.getDayOfWeek(), placement.getPeriodId(),
                    placement.getPeriodNumber(), placement.getTeachingIndex(),
                    placement.getStartTime(), placement.getEndTime());
        }

        void restore(Placement placement) {
            placement.setDayOfWeek(day);
            placement.setPeriodId(periodId);
            placement.setPeriodNumber(periodNumber);
            placement.setTeachingIndex(teachingIndex);
            placement.setStartTime(start);
            placement.setEndTime(end);
        }
    }
}
