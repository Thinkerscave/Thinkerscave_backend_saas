package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component
public class CandidateScorer {

    private static final double PREFERENCE_MATCH_SCORE = 50.0;
    private static final double SPREAD_SCORE = 30.0;
    private static final double CONSECUTIVE_PENALTY = -20.0;
    private static final double WORKLOAD_BALANCE_SCORE = 15.0;
    private static final double RESOURCE_PREFERENCE_SCORE = 10.0;
    private static final double GAP_PENALTY = -5.0;
    private static final double TIE_BREAKER_MAX = 3.0;

    public void scoreCandidates(SchedulingRequirement req,
                                SchedulingSolution solution,
                                SchedulingSourceData source,
                                Random random) {
        int halfIndex = source.getHalfIndex();
        List<DayOfWeek> workingDays = source.getWorkingDays();
        int maxWeekly = source.getMaxTeacherWeeklyPeriods();

        for (CandidateSlot slot : req.getCandidates()) {
            double score = 0.0;

            score += scorePreference(req.getPreference(), slot.getPeriodNumber(), halfIndex);
            score += scoreSpread(req, slot, solution, workingDays);
            score += scoreConsecutive(req, slot, solution);
            score += scoreTeacherBalance(slot, solution, workingDays, maxWeekly);
            score += scoreResource(req, slot);
            score += scoreTieBreaker(random);

            slot.setScore(score);
        }
    }

    private double scorePreference(SubjectTimetablePreference pref, short periodNumber, int halfIndex) {
        if (pref == SubjectTimetablePreference.ANY) return 0;
        if (pref == SubjectTimetablePreference.FIRST_HALF && periodNumber <= halfIndex) {
            return PREFERENCE_MATCH_SCORE;
        }
        if (pref == SubjectTimetablePreference.SECOND_HALF && periodNumber > halfIndex) {
            return PREFERENCE_MATCH_SCORE;
        }
        return -PREFERENCE_MATCH_SCORE * 0.3;
    }

    private double scoreSpread(SchedulingRequirement req, CandidateSlot slot,
                               SchedulingSolution solution, List<DayOfWeek> workingDays) {
        int existingInDay = solution.countPlacementsForSubjectInDay(
                req.getSectionId(), req.getClassSubjectMappingId(), slot.getDayOfWeek());
        if (existingInDay == 0) return SPREAD_SCORE;
        return -SPREAD_SCORE * existingInDay;
    }

    private double scoreConsecutive(SchedulingRequirement req, CandidateSlot slot,
                                    SchedulingSolution solution) {
        if (solution.hasConsecutiveSubject(req.getSectionId(), req.getClassSubjectMappingId(),
                slot.getDayOfWeek(), slot.getPeriodNumber())) {
            return CONSECUTIVE_PENALTY;
        }
        return 0;
    }

    private double scoreTeacherBalance(CandidateSlot slot, SchedulingSolution solution,
                                       List<DayOfWeek> workingDays, int maxWeekly) {
        int dayLoad = solution.getTeacherDayLoad(slot.getStaffId(), slot.getDayOfWeek());
        int idealDayLoad = maxWeekly / Math.max(workingDays.size(), 1);
        if (dayLoad < idealDayLoad) return WORKLOAD_BALANCE_SCORE;
        if (dayLoad > idealDayLoad + 1) return -WORKLOAD_BALANCE_SCORE;
        return 0;
    }

    private double scoreResource(SchedulingRequirement req, CandidateSlot slot) {
        if (req.getDefaultResourceId() != null && req.getDefaultResourceId().equals(slot.getResourceId())) {
            return RESOURCE_PREFERENCE_SCORE;
        }
        return 0;
    }

    private double scoreTieBreaker(Random random) {
        return random.nextDouble() * TIE_BREAKER_MAX;
    }
}
