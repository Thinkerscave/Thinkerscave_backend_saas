package com.thinkerscave.student.dto;

import com.thinkerscave.student.enums.StudentStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentSearchRequest {

    private String keyword;

    private Long classId;

    private Long sectionId;

    private StudentStatus status;

    private String parentName;
}