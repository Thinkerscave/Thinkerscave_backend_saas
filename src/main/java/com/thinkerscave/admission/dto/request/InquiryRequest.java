package com.thinkerscave.admission.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Create or update a prospect inquiry")
public class InquiryRequest {

    @NotBlank(message = "Prospect name is required")
    private String name;

    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;

    private String email;

    @NotBlank(message = "Class interested in is required")
    private String classInterestedIn;

    private String address;
    private String inquirySource;
    private String referredBy;
    private String comments;
    private Long assignedCounselorId;
    private LocalDate nextFollowUpDate;
}
