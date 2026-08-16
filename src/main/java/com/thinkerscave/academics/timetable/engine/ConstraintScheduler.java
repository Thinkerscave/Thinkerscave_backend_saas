package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ConstraintScheduler {

    private static final Logger log = LoggerFactory.getLogger(ConstraintScheduler.class);
    public static final int MAX_BACKTRACKS = 100_000;

    private final CandidateScorer scorer;

    public ConstraintScheduler(CandidateScorer scorer) {
        this.scorer = scorer;
    }

    public SchedulingSolution solve(List<SchedulingRequirement> requirements,
                                    SchedulingSourceData source,
                                    long seed,
                                    GenerationProgress progress) {
        Random random = new Random(seed);
        SchedulingSolution solution = new SchedulingSolution();

        List<SchedulingRequirement> ordered = sortByConstraintTightness(requirements);

        int totalBacktracks = 0;
        List<PlacementRecord> placementStack = new ArrayList<>();

        int i = 0;
        while (i < ordered.size()) {
            if (progress.isCancelRequested()) {
                log.info("Generation cancelled at requirement {}/{}", i, ordered.size());
                for (int j = i; j < ordered.size(); j++) {
                    solution.getUnplacedRequirements().add(ordered.get(j));
                }
                break;
            }

            SchedulingRequirement req = ordered.get(i);

            scorer.scoreCandidates(req, solution, source, random);
            List<CandidateSlot> feasible = filterAndSort(req, solution, source);

            int startIdx = 0;
            if (placementStack.size() > i) {
                PlacementRecord existing = placementStack.get(i);
                startIdx = existing.nextCandidateIdx;
                if (existing.placement != null) {
                    solution.unplace(existing.placement);
                    existing.placement = null;
                }
            }

            boolean placed = false;
            for (int ci = startIdx; ci < feasible.size(); ci++) {
                CandidateSlot slot = feasible.get(ci);
                if (isHardViolation(slot, req, solution, source)) continue;

                Placement p = new Placement(req, slot);
                solution.place(p);

                if (placementStack.size() > i) {
                    PlacementRecord rec = placementStack.get(i);
                    rec.placement = p;
                    rec.nextCandidateIdx = ci + 1;
                } else {
                    placementStack.add(new PlacementRecord(p, ci + 1));
                }
                placed = true;
                break;
            }

            if (placed) {
                i++;
                int pct = 40 + (int) ((i / (double) ordered.size()) * 30);
                progress.advanceTo(GenerationPhase.SCHEDULING, Math.min(pct, 70));
            } else {
                if (placementStack.size() > i) {
                    placementStack.remove(i);
                }

                if (i == 0 || totalBacktracks >= MAX_BACKTRACKS) {
                    for (int j = i; j < ordered.size(); j++) {
                        solution.getUnplacedRequirements().add(ordered.get(j));
                    }
                    log.warn("Scheduling incomplete: {} unplaced after {} backtracks",
                            ordered.size() - i, totalBacktracks);
                    break;
                }

                totalBacktracks++;
                i--;
            }
        }

        log.info("Scheduling done: {} placed, {} unplaced, {} backtracks",
                solution.getPlacements().size(),
                solution.getUnplacedRequirements().size(),
                totalBacktracks);

        return solution;
    }

    List<SchedulingRequirement> sortByConstraintTightness(List<SchedulingRequirement> requirements) {
        List<SchedulingRequirement> sorted = new ArrayList<>(requirements);
        sorted.sort(Comparator
                .<SchedulingRequirement, Integer>comparing(SchedulingRequirement::getStaticCandidateCount)
                .thenComparing(r -> r.getPreference() != SubjectTimetablePreference.ANY ? 0 : 1)
                .thenComparing(Comparator.<SchedulingRequirement, Integer>comparing(
                        SchedulingRequirement::getWeeklyPeriods).reversed())
                .thenComparing(SchedulingRequirement::getRequirementId));
        return sorted;
    }

    private List<CandidateSlot> filterAndSort(SchedulingRequirement req,
                                              SchedulingSolution solution,
                                              SchedulingSourceData source) {
        List<CandidateSlot> feasible = new ArrayList<>();
        for (CandidateSlot slot : req.getCandidates()) {
            if (!isHardViolation(slot, req, solution, source)) {
                feasible.add(slot);
            }
        }
        feasible.sort(Comparator.comparingDouble(CandidateSlot::getScore).reversed());
        return feasible;
    }

    private boolean isHardViolation(CandidateSlot slot, SchedulingRequirement req,
                                    SchedulingSolution solution, SchedulingSourceData source) {
        if (solution.isSectionOccupied(req.getSectionId(), slot.getDayOfWeek(), slot.getPeriodId())) {
            return true;
        }
        if (solution.isTeacherOccupied(slot.getStaffId(), slot.getDayOfWeek(), slot.getPeriodId())) {
            return true;
        }
        if (slot.getResourceId() != null
                && solution.isResourceOccupied(slot.getResourceId(), slot.getDayOfWeek(), slot.getPeriodId())) {
            return true;
        }
        if (solution.getTeacherWeeklyLoad(slot.getStaffId()) >= source.getMaxTeacherWeeklyPeriods()) {
            return true;
        }
        return false;
    }

    private static class PlacementRecord {
        Placement placement;
        int nextCandidateIdx;

        PlacementRecord(Placement placement, int nextCandidateIdx) {
            this.placement = placement;
            this.nextCandidateIdx = nextCandidateIdx;
        }
    }
}
