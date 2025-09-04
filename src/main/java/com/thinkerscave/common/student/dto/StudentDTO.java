package com.thinkerscave.common.student.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class StudentDTO {
	

    private Long studentId;

    
    private String firstName;

    
    private String middleName;

    
    private String lastName;

   
    private String email;

    
    private Long mobileNumber;

    
    private String gender;

    private boolean isSameAddress;
    
    private LocalDate dateOfBirth;

    private LocalDate enrollmentDate;

    private String rollNumber;

   
    private String remarks;

   
    private String photoUrl;


    private boolean isActive;

}
