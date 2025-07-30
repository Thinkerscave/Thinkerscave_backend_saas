package com.thinkerscave.common.staff.dto;


import lombok.*;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StaffRequestDTO {

    private Long staffId;
    private Long userId;
    private Long branchId;
    private Long departmentId;

    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private Long mobileNumber;
    private String gender;
    private LocalDate dateOfBirth;
    private LocalDate hireDate;
    private String photoUrl;
    private String address;
    private Boolean isActive;
}

