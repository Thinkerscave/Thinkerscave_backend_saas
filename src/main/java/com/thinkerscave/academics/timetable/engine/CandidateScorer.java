package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.SubjectTimetablePreference;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class CandidateScorer {

    private static final double PREFERENCE_MATCH_SCORE = 40.0;
    private static final double PREFERENCE_MISS_PENALTY = -60.0;
    private static final double FIRST_OF_DAY_SCORE = 30.0;
    private static final double WITHIN_DAILY_CAP_SCORE = 5.0;
    private static final double OVER_DAILY_CAP_PENALTY = -120.0;
    private static final double CONSECUTIVE_PENALTY = -20.0;
    private static final double WORKLOAD_BALANCE_SCORE = 15.0;
    private static final double TIE_BREAKER_MAX = 3.0;

    public double score(SchedulingRequirement req, CandidateSlot slot,
                        SchedulingSolution solution, SchedulingSourceData source, Random random) {
        double score = 0.0;
        score += scorePreference(req.getPreference(), slot.getTeachingIndex(), source.getHalfIndex());
        score += scoreSpread(req, slot, solution, source);
        score += scoreConsecutive(req, slot, solution, source);
        score += scoreTeacherBalance(slot, solution, source);
        score += random.nextDouble() * TIE_BREAKER_MAX;
        return score;
    }

    /** Highest number of periods of one subject a section should get on a single day. */
    static int dailyCap(int weeklyPeriods, int workingDays) {
        return Math.max(1, (int) Math.ceil(weeklyPeriods / (double) Math.max(workingDays, 1)));
    }

    private double scorePreference(SubjectTimetablePreference pref, int teachingIndex, int halfIndex) {
        if (pref == null || pref == SubjectTimetablePreference.ANY) return 0;
        boolean firstHalf = teachingIndex < halfIndex;
        if (pref == SubjectTimetablePreference.FIRST_HALF) {
            return firstHalf ? PREFERENCE_MATCH_SCORE : PREFERENCE_MISS_PENALTY;
        }
        if (pref == SubjectTimetablePreference.SECOND_HALF) {
            return firstHalf ? PREFERENCE_MISS_PENALTY : PREFERENCE_MATCH_SCORE;
        }
        return 0;
    }

    private double scoreSpread(SchedulingRequirement req, CandidateSlot slot,
                               SchedulingSolution solution, SchedulingSourceData source) {
        int existing = solution.countPlacementsForSubjectInDay(
                req.getSectionId(), req.getAcademicGroupSubjectMappingId(), slot.getDayOfWeek());
        int cap = dailyCap(req.getWeeklyPeriods(), source.workingDayCount());
        if (existing == 0) return FIRST_OF_DAY_SCORE;
        if (existing < cap) return WITHIN_DAILY_CAP_SCORE;
        return OVER_DAILY_CAP_PENALTY * (existing - cap + 1);
    }

    private double scoreConsecutive(SchedulingRequirement req, CandidateSlot slot,
                                    SchedulingSolution solution, SchedulingSourceData source) {
        if (!solution.hasConsecutiveSubject(req.getSectionId(), req.getAcademicGroupSubjectMappingId(),
                slot.getDayOfWeek(), slot.getPeriodNumber())) {
            return 0;
        }
        return req.getWeeklyPeriods() > source.workingDayCount() ? CONSECUTIVE_PENALTY / 4 : CONSECUTIVE_PENALTY;
    }

    private double scoreTeacherBalance(CandidateSlot slot, SchedulingSolution solution, SchedulingSourceData source) {
        int dayLoad = solution.getTeacherDayLoad(slot.getStaffId(), slot.getDayOfWeek());
        int weekly = solution.getTeacherWeeklyLoad(slot.getStaffId());
        int idealDayLoad = (int) Math.ceil((weekly + 1) / (double) source.workingDayCount());
        if (dayLoad < idealDayLoad) return WORKLOAD_BALANCE_SCORE;
        if (dayLoad > idealDayLoad + 1) return -WORKLOAD_BALANCE_SCORE;
        return 0;
    }
}
