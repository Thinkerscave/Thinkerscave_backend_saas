package com.thinkerscave.academics.dto.response;

import com.thinkerscave.academics.enums.SubjectCategory;
import com.thinkerscave.academics.enums.TeacherAllocationStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TeacherAllocationRowResponse {
    private Long teacherAllocationId;
    private Long academicYearId;
    private Long classId;
    private String className;
    private String classCode;
    private Long sectionId;
    private String sectionName;
    private Long classSubjectMappingId;
    private Long subjectId;
    private String subjectName;
    private String subjectCode;
    private SubjectCategory subjectCategory;
    private Short weeklyPeriods;

    private Long primaryStaffId;
    private String primaryStaffName;
    private Integer primaryWorkloadAssigned;
    private Integer primaryWorkloadMax;
    private String primaryWorkloadStatus;

    private TeacherAllocationStatus status;
    private Boolean active;
}
