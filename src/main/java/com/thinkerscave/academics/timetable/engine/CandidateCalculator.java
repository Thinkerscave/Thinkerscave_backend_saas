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
        List<DayOfWeek> workingDays = source.getWorkingDays();
        List<TimetablePeriod> teachingPeriods = source.getTeachingPeriods();

        for (SchedulingRequirement req : requirements) {
            List<CandidateSlot> candidates = new ArrayList<>();

            for (DayOfWeek day : workingDays) {
                for (int idx = 0; idx < teachingPeriods.size(); idx++) {
                    TimetablePeriod period = teachingPeriods.get(idx);
                    candidates.add(new CandidateSlot(
                            day,
                            period.getTimetablePeriodId(),
                            period.getPeriodNumber(),
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
