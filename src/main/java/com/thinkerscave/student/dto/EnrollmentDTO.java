package com.thinkerscave.student.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnrollmentDTO {

    private Long enrollmentId;

    private Long academicYearId;

    private Long classId;

    private Long sectionId;

    private String academicYear;

    private String className;

    private String sectionName;

    private String rollNumber;

    private String status;

    private String enrollmentDate;
}