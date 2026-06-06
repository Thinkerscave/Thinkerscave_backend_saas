package com.thinkerscave.common.student.workspace.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class StudentDirectoryCard {
    private Long studentId;
    private String admissionNumber;
    private String fullName;
    private String rollNumber;
    private String className;
    private String sectionName;
    private String mobile;
    private String email;
    private String gender;
    private String photoUrl;
    private Boolean active;
    private String attendanceStatus;     // "PRESENT_TODAY" | "ABSENT_TODAY" | "PENDING"
    private LocalDate dateOfBirth;
    private String guardianName;
    private String guardianMobile;
}
