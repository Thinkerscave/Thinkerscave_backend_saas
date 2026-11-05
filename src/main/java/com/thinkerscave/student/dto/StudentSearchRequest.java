package com.thinkerscave.student.dto;

import com.thinkerscave.student.enums.StudentStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentSearchRequest {

    private String keyword;

    private String rollNumber;

    private String parentMobile;

    private Long academicYearId;

    private Long classId;

    private Long sectionId;

    /** null = all, true = has active enrollment, false = has no active enrollment. */
    private Boolean assigned;

    private StudentStatus status;

    private String parentName;
}