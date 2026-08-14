package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.DayOfWeek;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CandidateSlot {

    private DayOfWeek dayOfWeek;
    private Long periodId;
    private short periodNumber;
    private Long staffId;
    private Long resourceId;
    private double score;

    public CandidateSlot(DayOfWeek dayOfWeek, Long periodId, short periodNumber,
                         Long staffId, Long resourceId) {
        this.dayOfWeek = dayOfWeek;
        this.periodId = periodId;
        this.periodNumber = periodNumber;
        this.staffId = staffId;
        this.resourceId = resourceId;
    }
}
