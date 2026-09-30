package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Swaps pairs of lessons within the same section when that improves subject spread and
 * first/second-half preferences without introducing any hard clash.
 */
@Component
public class SoftConstraintOptimizer {

    private static final Logger log = LoggerFactory.getLogger(SoftConstraintOptimizer.class);
    private static final int MAX_OPTIMIZATION_PASSES = 3;

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

        Map<Long, List<Placement>> bySection = new HashMap<>();
        for (Placement p : solution.getPlacements()) {
            bySection.computeIfAbsent(p.getSectionId(), k -> new ArrayList<>()).add(p);
        }

        int improvementCount = 0;
        for (int pass = 0; pass < MAX_OPTIMIZATION_PASSES; pass++) {
            if (progress.isCancelRequested()) break;
            int passImprovements = 0;
            for (List<Placement> sectionPlacements : bySection.values()) {
                passImprovements += runSwapPass(sectionPlacements, solution, source, requirementsById);
            }
            improvementCount += passImprovements;
            if (passImprovements == 0) break;
        }

        progress.advanceTo(GenerationPhase.OPTIMIZING, 80);
        log.info("Optimization complete: {} swaps applied", improvementCount);
        return solution;
    }

    private int runSwapPass(List<Placement> placements, SchedulingSolution solution,
                            SchedulingSourceData source, Map<String, SchedulingRequirement> requirementsById) {
        int improvements = 0;
        for (int a = 0; a < placements.size(); a++) {
            for (int b = a + 1; b < placements.size(); b++) {
                Placement pA = placements.get(a);
                Placement pB = placements.get(b);
                if (Objects.equals(pA.getClassSubjectMappingId(), pB.getClassSubjectMappingId())) continue;
                if (pA.getDayOfWeek() == pB.getDayOfWeek() && pA.getPeriodId().equals(pB.getPeriodId())) continue;

                double before = evaluate(pA, solution, source, requirementsById)
                        + evaluate(pB, solution, source, requirementsById);
                if (!canSwapSlots(pA, pB, solution)) continue;

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

    private boolean canSwapSlots(Placement pA, Placement pB, SchedulingSolution solution) {
        solution.unplace(pA);
        solution.unplace(pB);

        boolean canPlaceAinB = !solution.isSectionOccupied(pA.getSectionId(), pB.getDayOfWeek(), pB.getPeriodId())
                && !solution.isTeacherOccupied(pA.getStaffId(), pB.getDayOfWeek(), pB.getPeriodId())
                && (pA.getResourceId() == null || !solution.isResourceOccupied(pA.getResourceId(), pB.getDayOfWeek(), pB.getPeriodId()));

        boolean canPlaceBinA = !solution.isSectionOccupied(pB.getSectionId(), pA.getDayOfWeek(), pA.getPeriodId())
                && !solution.isTeacherOccupied(pB.getStaffId(), pA.getDayOfWeek(), pA.getPeriodId())
                && (pB.getResourceId() == null || !solution.isResourceOccupied(pB.getResourceId(), pA.getDayOfWeek(), pA.getPeriodId()));

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

        pA.setDayOfWeek(pB.getDayOfWeek());
        pA.setPeriodId(pB.getPeriodId());
        pA.setPeriodNumber(pB.getPeriodNumber());
        pA.setTeachingIndex(pB.getTeachingIndex());

        pB.setDayOfWeek(dayA);
        pB.setPeriodId(periodIdA);
        pB.setPeriodNumber(periodNumA);
        pB.setTeachingIndex(teachingIdxA);

        solution.place(pA);
        solution.place(pB);
    }

    private double evaluate(Placement p, SchedulingSolution solution, SchedulingSourceData source,
                            Map<String, SchedulingRequirement> requirementsById) {
        SchedulingRequirement req = requirementsById.get(p.getRequirementId());
        int weekly = req == null ? 1 : req.getWeeklyPeriods();
        int cap = CandidateScorer.dailyCap(weekly, source.workingDayCount());
        int sameDay = solution.countPlacementsForSubjectInDay(
                p.getSectionId(), p.getClassSubjectMappingId(), p.getDayOfWeek());
        double score = sameDay > cap ? -40.0 * (sameDay - cap) : 0;

        if (req != null && req.getPreference() != null && req.getPreference() != SubjectTimetablePreference.ANY) {
            boolean firstHalf = p.getTeachingIndex() < source.getHalfIndex();
            boolean wantsFirst = req.getPreference() == SubjectTimetablePreference.FIRST_HALF;
            score += firstHalf == wantsFirst ? 10.0 : -10.0;
        }
        return score;
    }
}
