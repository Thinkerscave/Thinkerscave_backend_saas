package com.thinkerscave.common.student.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class StudentResponseDTO {
    private Long studentId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private Long mobileNumber;
    private String gender;
    private LocalDate dateOfBirth;
    private LocalDate enrollmentDate;
    private String rollNumber;
    private String remarks;
    private boolean isActive;

    private Long classId;
    private String className;
    private Long sectionId;
    private String sectionName;
    private String parentName;
}
