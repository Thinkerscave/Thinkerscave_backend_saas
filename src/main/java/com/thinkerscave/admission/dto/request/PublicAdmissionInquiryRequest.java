package com.thinkerscave.admission.dto.request;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PublicAdmissionInquiryRequest {

    @Size(max = 100, message = "Student name is too long")
    private String studentName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number")
    private String mobileNumber;

    @NotNull(message = "Please select a class")
    private Long classId;

    @NotNull(message = "Please accept the consent to be contacted")
    @AssertTrue(message = "Please accept the consent to be contacted")
    private Boolean consent;
}