package com.thinkerscave.common.student.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AlumniRequest {

    @NotBlank
    private String fullName;

    private Long studentId;
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
