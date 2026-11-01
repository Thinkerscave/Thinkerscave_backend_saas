package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.academics.enums.TimetableEntryType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class Placement {

    private String requirementId;
    private Long unitId;
    private Long sectionId;
    private Long academicGroupId;
    private Long academicGroupSubjectMappingId;
    private Long choiceMappingId;
    private int periodIndex;
    private Long subjectId;
    private Long teacherAllocationId;
    private Long staffId;
    private Long resourceId;
    private Long fixedActivityId;
    private DayOfWeek dayOfWeek;
    private Long periodId;
    private short periodNumber;
    private LocalTime startTime;
    private LocalTime endTime;
    private int teachingIndex;
    private TimetableEntryType entryType = TimetableEntryType.SUBJECT;
    private String subjectNameSnapshot;
    private boolean locked;

    public Placement() {
    }

    public Placement(SchedulingRequirement req, CandidateSlot slot) {
        this.requirementId = req.getRequirementId();
        this.unitId = req.getUnitId();
        this.sectionId = req.getSectionId();
        this.academicGroupId = req.getAcademicGroupId();
        this.academicGroupSubjectMappingId = req.getAcademicGroupSubjectMappingId();
        this.choiceMappingId = req.getChoiceMappingId();
        this.periodIndex = req.getPeriodIndex();
        this.subjectId = req.getSubjectId();
        this.teacherAllocationId = req.getTeacherAllocationId();
        this.staffId = slot.getStaffId();
        this.resourceId = slot.getResourceId();
        this.dayOfWeek = slot.getDayOfWeek();
        this.periodId = slot.getPeriodId();
        this.periodNumber = slot.getPeriodNumber();
        this.startTime = slot.getStartTime();
        this.endTime = slot.getEndTime();
        this.teachingIndex = slot.getTeachingIndex();
        this.subjectNameSnapshot = req.getSubjectName();
        this.entryType = TimetableEntryType.SUBJECT;
    }

    public void moveTo(CandidateSlot slot) {
        this.dayOfWeek = slot.getDayOfWeek();
        this.periodId = slot.getPeriodId();
        this.periodNumber = slot.getPeriodNumber();
        this.startTime = slot.getStartTime();
        this.endTime = slot.getEndTime();
        this.teachingIndex = slot.getTeachingIndex();
    }

    public int startMinute() {
        return startTime == null ? 0 : startTime.getHour() * 60 + startTime.getMinute();
    }

    public int endMinute() {
        return endTime == null ? 0 : endTime.getHour() * 60 + endTime.getMinute();
    }
}
