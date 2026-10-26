package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Places every requirement without section, teacher, room or weekly-load clashes.
 *
 * <p>Each attempt repeatedly schedules the most constrained remaining requirement into its
 * best-scoring free slot. When no free slot exists it tries a one-step repair: move the
 * lessons blocking a slot to another free slot. Attempts restart with a different seed until
 * a complete schedule is found or {@link #MAX_ATTEMPTS} is reached; the best attempt wins.
 */
@Component
public class ConstraintScheduler {

    private static final Logger log = LoggerFactory.getLogger(ConstraintScheduler.class);
    public static final int MAX_ATTEMPTS = 12;

    private final CandidateScorer scorer;

    public ConstraintScheduler(CandidateScorer scorer) {
        this.scorer = scorer;
    }

    public SchedulingSolution solve(List<SchedulingRequirement> requirements,
                                    SchedulingSourceData source,
                                    long seed,
                                    GenerationProgress progress) {
        return solve(requirements, source, seed, progress, List.of());
    }

    public SchedulingSolution solve(List<SchedulingRequirement> requirements,
                                    SchedulingSourceData source,
                                    long seed,
                                    GenerationProgress progress,
                                    List<Placement> reservations) {
        Map<String, SchedulingRequirement> byId = new HashMap<>();
        Map<Long, Integer> teacherDemand = new HashMap<>();
        for (SchedulingRequirement r : requirements) {
            byId.put(r.getRequirementId(), r);
            teacherDemand.merge(r.getPrimaryStaffId(), 1, Integer::sum);
        }

        SchedulingSolution best = null;
        int attempts = 0;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            if (progress.isCancelRequested()) break;
            attempts++;
            Random random = new Random(seed + attempt * 7919L);
            SchedulingSolution candidate = runAttempt(requirements, byId, teacherDemand, source, random,
                    progress, attempt == 0, reservations);
            if (best == null
                    || candidate.getUnplacedRequirements().size() < best.getUnplacedRequirements().size()) {
                best = candidate;
            }
            log.info("Scheduling attempt {}: {} placed, {} unplaced", attempt + 1,
                    candidate.getPlacements().size(), candidate.getUnplacedRequirements().size());
            if (best.isComplete()) break;
            progress.advanceTo(GenerationPhase.SCHEDULING, Math.min(70, 55 + attempt));
        }

        if (best == null) {
            best = new SchedulingSolution();
            best.getUnplacedRequirements().addAll(requirements);
        }
        log.info("Scheduling done after {} attempt(s): {} placed, {} unplaced",
                attempts, best.getPlacements().size(), best.getUnplacedRequirements().size());
        return best;
    }

    private SchedulingSolution runAttempt(List<SchedulingRequirement> requirements,
                                          Map<String, SchedulingRequirement> byId,
                                          Map<Long, Integer> teacherDemand,
                                          SchedulingSourceData source,
                                          Random random,
                                          GenerationProgress progress,
                                          boolean reportProgress,
                                          List<Placement> reservations) {
        SchedulingSolution solution = new SchedulingSolution();
        reservations.forEach(solution::place);
        List<SchedulingRequirement> remaining = new ArrayList<>(requirements);
        Collections.shuffle(remaining, random);
        int total = requirements.size();

        while (!remaining.isEmpty()) {
            if (progress.isCancelRequested()) {
                solution.getUnplacedRequirements().addAll(remaining);
                break;
            }

            int pick = pickMostConstrained(remaining, solution, source, teacherDemand);
            SchedulingRequirement req = remaining.remove(pick);

            CandidateSlot slot = bestSlot(req, solution, source, random);
            if (slot != null) {
                solution.place(new Placement(req, slot));
            } else if (!repair(req, solution, source, random, byId)) {
                solution.getUnplacedRequirements().add(req);
            }

            if (reportProgress && (total - remaining.size()) % 25 == 0) {
                int pct = 40 + (int) (((total - remaining.size()) / (double) total) * 15);
                progress.advanceTo(GenerationPhase.SCHEDULING, Math.min(pct, 55));
            }
        }
        return solution;
    }

    /** Index of the remaining requirement with the fewest free slots (ties: busiest teacher, preference set). */
    private int pickMostConstrained(List<SchedulingRequirement> remaining,
                                    SchedulingSolution solution,
                                    SchedulingSourceData source,
                                    Map<Long, Integer> teacherDemand) {
        Map<String, Integer> feasibleByOwner = new HashMap<>();
        int bestIdx = 0;
        int bestCount = Integer.MAX_VALUE;
        for (int i = 0; i < remaining.size(); i++) {
            SchedulingRequirement r = remaining.get(i);
            String owner = r.getUnitId() + "|" + r.getPrimaryStaffId() + "|" + r.getDefaultResourceId();
            int count = feasibleByOwner.computeIfAbsent(owner, k -> countFeasible(r, solution, source));
            if (count < bestCount || (count == bestCount && isTighter(r, remaining.get(bestIdx), teacherDemand))) {
                bestCount = count;
                bestIdx = i;
            }
        }
        return bestIdx;
    }

    private boolean isTighter(SchedulingRequirement a, SchedulingRequirement b, Map<Long, Integer> teacherDemand) {
        int demandA = teacherDemand.getOrDefault(a.getPrimaryStaffId(), 0);
        int demandB = teacherDemand.getOrDefault(b.getPrimaryStaffId(), 0);
        if (demandA != demandB) return demandA > demandB;
        boolean prefA = a.getPreference() != null && a.getPreference() != SubjectTimetablePreference.ANY;
        boolean prefB = b.getPreference() != null && b.getPreference() != SubjectTimetablePreference.ANY;
        return prefA && !prefB;
    }

    private int countFeasible(SchedulingRequirement req, SchedulingSolution solution, SchedulingSourceData source) {
        if (solution.getTeacherWeeklyLoad(req.getPrimaryStaffId()) >= source.getMaxTeacherWeeklyPeriods()) {
            return 0;
        }
        int count = 0;
        for (CandidateSlot slot : req.getCandidates()) {
            if (!isHardViolation(slot, req, solution, source)) count++;
        }
        return count;
    }

    private CandidateSlot bestSlot(SchedulingRequirement req, SchedulingSolution solution,
                                   SchedulingSourceData source, Random random) {
        CandidateSlot best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (CandidateSlot slot : req.getCandidates()) {
            if (isHardViolation(slot, req, solution, source)) continue;
            double score = scorer.score(req, slot, solution, source, random);
            if (score > bestScore) {
                bestScore = score;
                best = slot;
            }
        }
        return best;
    }

    private record Position(DayOfWeek day, Long periodId, short periodNumber, int teachingIndex,
                            java.time.LocalTime startTime, java.time.LocalTime endTime) {
        static Position of(Placement p) {
            return new Position(p.getDayOfWeek(), p.getPeriodId(), p.getPeriodNumber(), p.getTeachingIndex(),
                    p.getStartTime(), p.getEndTime());
        }

        CandidateSlot asSlot(Placement p) {
            return new CandidateSlot(day, p.getUnitId(), p.getSectionId(), p.getAcademicGroupId(),
                    periodId, periodNumber, startTime, endTime, teachingIndex,
                    p.getStaffId(), p.getResourceId());
        }
    }

    /**
     * Frees a slot for {@code req} by moving the (at most two) lessons occupying it to other
     * free slots. Leaves the solution unchanged when no such move exists.
     */
    private boolean repair(SchedulingRequirement req, SchedulingSolution solution,
                           SchedulingSourceData source, Random random,
                           Map<String, SchedulingRequirement> byId) {
        if (solution.getTeacherWeeklyLoad(req.getPrimaryStaffId()) >= source.getMaxTeacherWeeklyPeriods()) {
            return false;
        }
        List<CandidateSlot> slots = new ArrayList<>(req.getCandidates());
        Collections.shuffle(slots, random);

        for (CandidateSlot slot : slots) {
            Set<Placement> blockers = Collections.newSetFromMap(new IdentityHashMap<>());
            addIfPresent(blockers, solution.placementAtSection(req.getUnitId(), slot.getDayOfWeek(), slot.getPeriodId()));
            addIfPresent(blockers, solution.placementAtTeacher(slot.getStaffId(), slot.getDayOfWeek(), slot.getPeriodId()));
            addIfPresent(blockers, solution.placementAtResource(slot.getResourceId(), slot.getDayOfWeek(), slot.getPeriodId()));
            if (blockers.isEmpty() || blockers.size() > 2) continue;

            Map<Placement, Position> original = new IdentityHashMap<>();
            for (Placement b : blockers) {
                original.put(b, Position.of(b));
                solution.unplace(b);
            }
            Placement placed = new Placement(req, slot);
            solution.place(placed);

            List<Placement> moved = new ArrayList<>();
            boolean ok = true;
            for (Placement b : blockers) {
                SchedulingRequirement blockerReq = byId.get(b.getRequirementId());
                CandidateSlot target = blockerReq == null ? null : bestSlot(blockerReq, solution, source, random);
                if (target == null) {
                    ok = false;
                    break;
                }
                b.moveTo(target);
                solution.place(b);
                moved.add(b);
            }
            if (ok) return true;

            for (Placement m : moved) solution.unplace(m);
            solution.unplace(placed);
            for (Placement b : blockers) {
                b.moveTo(original.get(b).asSlot(b));
                solution.place(b);
            }
        }
        return false;
    }

    private static void addIfPresent(Set<Placement> set, Placement p) {
        if (p != null) set.add(p);
    }

    static boolean isHardViolation(CandidateSlot slot, SchedulingRequirement req,
                                   SchedulingSolution solution, SchedulingSourceData source) {
        if (solution.isSectionOccupied(req.getUnitId(), slot.getDayOfWeek(),
                slot.getStartTime(), slot.getEndTime())) return true;
        if (solution.isTeacherOccupied(slot.getStaffId(), slot.getDayOfWeek(),
                slot.getStartTime(), slot.getEndTime())) return true;
        if (slot.getResourceId() != null
                && solution.isResourceOccupied(slot.getResourceId(), slot.getDayOfWeek(),
                slot.getStartTime(), slot.getEndTime())) {
            return true;
        }
        return solution.getTeacherWeeklyLoad(slot.getStaffId()) >= source.getMaxTeacherWeeklyPeriods();
    }
}
