package com.thinkerscave.staff.dto.request;

import com.thinkerscave.staff.enums.EmploymentCategory;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.StaffType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class StaffCreateRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String middleName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100)
    private String lastName;

    @NotBlank(message = "Gender is required")
    @Size(max = 20)
    private String gender;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    @Size(max = 10)
    private String bloodGroup;

    @Size(max = 50)
    private String religion;

    @Size(max = 50)
    private String nationality;

    @NotBlank(message = "Mobile number is required")
    @Size(max = 15)
    private String mobileNumber;

    @NotBlank(message = "Email is required")
    @Email
    @Size(max = 150)
    private String email;

    @NotNull(message = "Staff type is required")
    private StaffType staffType;

    @NotBlank(message = "Designation is required")
    @Size(max = 100)
    private String designation;

    @NotNull(message = "Employment category is required")
    private EmploymentCategory employmentCategory;

    @NotNull(message = "Employment status is required")
    private EmploymentStatus employmentStatus;

    @NotNull(message = "Joining date is required")
    private LocalDate joiningDate;

    @Size(max = 255)
    private String highestQualification;

    @Min(0)
    private Integer experienceYears;

    @Size(max = 150)
    private String emergencyContactName;

    @Size(max = 100)
    private String emergencyContactRelation;

    @Size(max = 15)
    private String emergencyContactNumber;

    private String photoUrl;

    private String remarks;
}
