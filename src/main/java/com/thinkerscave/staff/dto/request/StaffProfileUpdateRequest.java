package com.thinkerscave.staff.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StaffProfileUpdateRequest {

    @Size(max = 15)
    private String mobileNumber;

    @Size(max = 150)
    private String emergencyContactName;

    @Size(max = 100)
    private String emergencyContactRelation;

    @Size(max = 15)
    private String emergencyContactNumber;

    @Size(max = 50)
    private String religion;

    @Size(max = 50)
    private String nationality;

    @Size(max = 10)
    private String bloodGroup;

    private String photoUrl;
}
