package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.entity.TimetablePeriod;
import com.thinkerscave.academics.enums.DayOfWeek;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CandidateCalculator {

    public void calculateCandidates(List<SchedulingRequirement> requirements,
                                    SchedulingSourceData source) {
        for (SchedulingRequirement req : requirements) {
            List<CandidateSlot> candidates = new ArrayList<>();
            for (DayOfWeek day : source.getWorkingDays()) {
                List<TimetablePeriod> teachingPeriods = source.slotsForUnitOnDay(req.getUnitId(), day);
                for (int idx = 0; idx < teachingPeriods.size(); idx++) {
                    TimetablePeriod period = teachingPeriods.get(idx);
                    candidates.add(new CandidateSlot(
                            day,
                            req.getUnitId(),
                            req.getSectionId(),
                            req.getAcademicGroupId(),
                            period.getTimetablePeriodId(),
                            period.getPeriodNumber(),
                            period.getStartTime(),
                            period.getEndTime(),
                            idx,
                            req.getPrimaryStaffId(),
                            req.getDefaultResourceId()
                    ));
                }
            }
            req.setCandidates(candidates);
            req.setStaticCandidateCount(candidates.size());
        }
    }
}
