package com.thinkerscave.student.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EnrollmentDTO {

    private Long enrollmentId;

    private String academicYear;

    private String className;

    private String sectionName;

    private String rollNumber;

    private String status;
}