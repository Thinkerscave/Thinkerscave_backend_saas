package com.thinkerscave.staff.dto.response;

import com.thinkerscave.access.dto.response.ResponsibilityAssignmentResponse;
import com.thinkerscave.shared.dto.AddressDto;
import com.thinkerscave.staff.enums.EmploymentCategory;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.StaffType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class StaffDetailResponse {

    // Basic Details
    private Long staffId;
    private String staffCode;
    private Long userId;
    private String firstName;
    private String middleName;
    private String lastName;
    private String fullName;
    private String gender;
    private LocalDate dateOfBirth;
    private String bloodGroup;
    private String religion;
    private String nationality;
    private String mobileNumber;
    private String email;
    private String photoUrl;

    // Professional Details
    private StaffType staffType;
    private String designation;
    private EmploymentCategory employmentCategory;
    private EmploymentStatus employmentStatus;
    private LocalDate joiningDate;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private String highestQualification;
    private Integer experienceYears;

    // Emergency Contact
    private String emergencyContactName;
    private String emergencyContactRelation;
    private String emergencyContactNumber;

    // Status
    private Boolean active;
    private String remarks;

    // Audit
    private LocalDateTime createdOn;
    private LocalDateTime updatedOn;

    // Subject slots (teaching staff; primary is always set for teaching staff)
    private SubjectRef primarySubject;
    private SubjectRef secondarySubject;

    public record SubjectRef(Long subjectId, String name) {}

    // Responsibility Summary
    private List<ResponsibilityAssignmentResponse> responsibilities;

    // Documents
    private List<DocumentResponse> documents;

    // Address
    private AddressDto presentAddress;
    private AddressDto permanentAddress;
    private Boolean sameAsPresentAddress;

    // Bank (masked; account number never returned)
    private StaffBankDetailsResponse bankDetails;
}
