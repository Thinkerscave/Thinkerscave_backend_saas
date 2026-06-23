package com.thinkerscave.staff.dto.response;

import com.thinkerscave.staff.enums.EmploymentCategory;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.StaffType;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Builder
public class StaffSummaryResponse {

    private Long staffId;
    private String staffCode;
    private String fullName;
    private String email;
    private String mobileNumber;
    private String photoUrl;
    private StaffType staffType;
    private String designation;
    private EmploymentCategory employmentCategory;
    private EmploymentStatus employmentStatus;
    private LocalDate joiningDate;
    private Boolean active;
}
