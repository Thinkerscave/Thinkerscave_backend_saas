package com.thinkerscave.common.course.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassTeacherAssignmentDTO {
    private Long assignmentId;
    private Long organizationId;
    private Long academicYearId;
    private Long classId;
    private String className;
    private Long sectionId;
    private String sectionName;
    private Long teacherId;
    private String teacherName;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Boolean isActive;
    private String notes;
}