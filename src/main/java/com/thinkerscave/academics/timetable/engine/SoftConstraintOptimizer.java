package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class SoftConstraintOptimizer {

    private static final Logger log = LoggerFactory.getLogger(SoftConstraintOptimizer.class);
    private static final int MAX_OPTIMIZATION_PASSES = 3;

    public SchedulingSolution optimize(SchedulingSolution solution,
                                       SchedulingSourceData source,
                                       GenerationProgress progress) {
        progress.advanceTo(GenerationPhase.OPTIMIZING, 72);

        int improvementCount = 0;
        for (int pass = 0; pass < MAX_OPTIMIZATION_PASSES; pass++) {
            int passImprovements = runSwapPass(solution, source);
            improvementCount += passImprovements;
            if (passImprovements == 0) break;
        }

        progress.advanceTo(GenerationPhase.OPTIMIZING, 80);
        log.info("Optimization complete: {} swaps applied", improvementCount);
        return solution;
    }

    private int runSwapPass(SchedulingSolution solution, SchedulingSourceData source) {
        List<Placement> placements = new ArrayList<>(solution.getPlacements());
        int improvements = 0;

        for (int a = 0; a < placements.size(); a++) {
            for (int b = a + 1; b < placements.size(); b++) {
                Placement pA = placements.get(a);
                Placement pB = placements.get(b);

                if (!pA.getSectionId().equals(pB.getSectionId())) continue;
                if (pA.getDayOfWeek() == pB.getDayOfWeek()
                        && pA.getPeriodId().equals(pB.getPeriodId())) continue;

                double currentScore = evaluatePair(pA, pB, solution, source);

                if (canSwapSlots(pA, pB, solution, source)) {
                    swapSlots(pA, pB, solution);
                    double newScore = evaluatePair(pA, pB, solution, source);

                    if (newScore > currentScore) {
                        improvements++;
                    } else {
                        swapSlots(pA, pB, solution);
                    }
                }
            }
        }
        return improvements;
    }

    private boolean canSwapSlots(Placement pA, Placement pB,
                                 SchedulingSolution solution, SchedulingSourceData source) {
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

        pA.setDayOfWeek(pB.getDayOfWeek());
        pA.setPeriodId(pB.getPeriodId());
        pA.setPeriodNumber(pB.getPeriodNumber());

        pB.setDayOfWeek(dayA);
        pB.setPeriodId(periodIdA);
        pB.setPeriodNumber(periodNumA);

        solution.place(pA);
        solution.place(pB);
    }

    private double evaluatePair(Placement pA, Placement pB,
                                SchedulingSolution solution, SchedulingSourceData source) {
        double score = 0;
        score += evaluateSpread(pA, solution);
        score += evaluateSpread(pB, solution);
        return score;
    }

    private double evaluateSpread(Placement p, SchedulingSolution solution) {
        int sameSubjectSameDay = solution.countPlacementsForSubjectInDay(
                p.getSectionId(), p.getClassSubjectMappingId(), p.getDayOfWeek());
        return -sameSubjectSameDay * 10.0;
    }
}
