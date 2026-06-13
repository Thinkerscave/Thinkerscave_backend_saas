package com.thinkerscave.student.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StudentResponseDTO {

    private Long studentId;

    private String studentCode;

    private String admissionNumber;

    private String fullName;

    private String mobileNumber;

    private String email;

    private String status;

    private String className;

    private String sectionName;

    private String parentName;

    private String parentMobileNumber;
}