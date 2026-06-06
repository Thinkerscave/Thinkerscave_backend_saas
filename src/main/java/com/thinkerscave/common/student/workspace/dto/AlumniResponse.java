package com.thinkerscave.common.student.workspace.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AlumniResponse {
    private Long alumniId;
    private Long studentId;
    private String fullName;
    private String batchYear;
    private String yearPassed;
    private String course;
    private String occupation;
    private String employer;
    private String contact;
    private String email;
    private String city;
    private LocalDate graduationDate;
    private String linkedIn;
}
