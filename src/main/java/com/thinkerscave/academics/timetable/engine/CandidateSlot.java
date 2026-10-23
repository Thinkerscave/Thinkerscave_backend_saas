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
    /** Zero-based position among the day's teaching periods (breaks excluded). */
    private int teachingIndex;
    private Long staffId;
    private Long resourceId;
    private double score;

    public CandidateSlot(DayOfWeek dayOfWeek, Long periodId, short periodNumber, int teachingIndex,
                         Long staffId, Long resourceId) {
        this.dayOfWeek = dayOfWeek;
        this.periodId = periodId;
        this.periodNumber = periodNumber;
        this.teachingIndex = teachingIndex;
        this.staffId = staffId;
        this.resourceId = resourceId;
    }
}
