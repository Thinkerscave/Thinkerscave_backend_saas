package com.thinkerscave.staff.dto.request;

import com.thinkerscave.shared.dto.AddressDto;
import com.thinkerscave.staff.enums.EmploymentCategory;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.StaffType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class StaffUpdateRequest {

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
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 10)
    private String bloodGroup;

    @Size(max = 50)
    private String religion;

    @Size(max = 50)
    private String nationality;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[0-9+\\- ]{7,15}$", message = "Mobile number is invalid")
    private String mobileNumber;

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

    private LocalDate contractStartDate;

    private LocalDate contractEndDate;

    @Size(max = 255)
    private String highestQualification;

    @Min(0)
    private Integer experienceYears;

    /** Same rules as create; the three slots replace the stored subjects. */
    private Long primarySubjectId;

    private Long secondarySubjectId;

    @Size(max = 150)
    private String emergencyContactName;

    @Size(max = 100)
    private String emergencyContactRelation;

    @Pattern(regexp = "^$|^[0-9+\\- ]{7,15}$", message = "Emergency contact number is invalid")
    private String emergencyContactNumber;

    private String remarks;

    @Valid
    private AddressDto presentAddress;

    @Valid
    private AddressDto permanentAddress;

    private Boolean sameAsPresentAddress;
}
