package com.thinkerscave.common.staff.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffResponseDTO {

    private Long id;
    private Long staffId;
    private String staffCode;
    private Long userId;
    private String userName;
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
    private String city;
    private String state;
    private String remarks;
    private Boolean isActive;
    private Long organizationId;
    private Long branchId;
    private String branchCode;
    private String branchName;
    private Long departmentId;
    private String departmentCode;
    private String departmentName;
}