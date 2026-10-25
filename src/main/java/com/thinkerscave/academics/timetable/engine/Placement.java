package com.thinkerscave.academics.timetable.engine;

import com.thinkerscave.academics.enums.DayOfWeek;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Placement {

    private String requirementId;
    private Long sectionId;
    private Long academicGroupSubjectMappingId;
    private Long teacherAllocationId;
    private Long staffId;
    private Long resourceId;
    private DayOfWeek dayOfWeek;
    private Long periodId;
    private short periodNumber;
    private int teachingIndex;
    private String subjectNameSnapshot;

    public Placement(SchedulingRequirement req, CandidateSlot slot) {
        this.requirementId = req.getRequirementId();
        this.sectionId = req.getSectionId();
        this.academicGroupSubjectMappingId = req.getAcademicGroupSubjectMappingId();
        this.teacherAllocationId = req.getTeacherAllocationId();
        this.staffId = slot.getStaffId();
        this.resourceId = slot.getResourceId();
        this.dayOfWeek = slot.getDayOfWeek();
        this.periodId = slot.getPeriodId();
        this.periodNumber = slot.getPeriodNumber();
        this.teachingIndex = slot.getTeachingIndex();
        this.subjectNameSnapshot = req.getSubjectName();
    }

    public void moveTo(CandidateSlot slot) {
        this.dayOfWeek = slot.getDayOfWeek();
        this.periodId = slot.getPeriodId();
        this.periodNumber = slot.getPeriodNumber();
        this.teachingIndex = slot.getTeachingIndex();
    }
}
