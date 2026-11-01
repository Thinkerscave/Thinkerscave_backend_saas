package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.DayOfWeek;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class CandidateSlot {

    private DayOfWeek dayOfWeek;
    private Long unitId;
    private Long sectionId;
    private Long academicGroupId;
    private Long periodId;
    private short periodNumber;
    private LocalTime startTime;
    private LocalTime endTime;
    /** Zero-based position among the day's teaching periods (breaks excluded). */
    private int teachingIndex;
    private Long staffId;
    private Long resourceId;
    private double score;

    public CandidateSlot(DayOfWeek dayOfWeek, Long unitId, Long sectionId, Long academicGroupId,
                         Long periodId, short periodNumber, LocalTime startTime, LocalTime endTime,
                         int teachingIndex, Long staffId, Long resourceId) {
        this.dayOfWeek = dayOfWeek;
        this.unitId = unitId;
        this.sectionId = sectionId;
        this.academicGroupId = academicGroupId;
        this.periodId = periodId;
        this.periodNumber = periodNumber;
        this.startTime = startTime;
        this.endTime = endTime;
        this.teachingIndex = teachingIndex;
        this.staffId = staffId;
        this.resourceId = resourceId;
    }

    public int startMinute() {
        return startTime == null ? 0 : startTime.getHour() * 60 + startTime.getMinute();
    }

    public int endMinute() {
        return endTime == null ? 0 : endTime.getHour() * 60 + endTime.getMinute();
    }
}
