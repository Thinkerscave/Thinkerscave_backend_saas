package com.thinkerscave.admission.dto.request;

import com.thinkerscave.admission.enums.LeadSource;
import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Create or update a prospect inquiry")
public class InquiryRequest {

    @JsonAlias({"studentName", "childName"})
    @NotBlank(message = "Student / child name is required")
    @Size(max = 100)
    private String name;

    @NotBlank(message = "Parent / contact name is required")
    @Size(max = 100)
    private String parentContactName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^(?:\\+91[\\s-]?)?[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number")
    private String mobileNumber;

    private String email;

    @NotBlank(message = "Class interested in is required")
    private String classInterestedIn;

    @NotNull(message = "Academic year is required")
    private Long academicYearId;

    @NotNull(message = "Class is required")
    private Long classId;
    private String address;

    @NotNull(message = "Lead source is required")
    private LeadSource inquirySource;

    private String referredBy;
    private String comments;
    private Long assignedCounselorId;
    private LocalDate nextFollowUpDate;
    private Boolean allowPotentialDuplicate;

    @AssertTrue(message = "Referred by is required when source is Referral")
    public boolean isReferralDetailsValid() {
        if (inquirySource != LeadSource.REFERRAL) {
            return true;
        }
        return referredBy != null && !referredBy.trim().isEmpty();
    }
}
