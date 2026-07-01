package com.thinkerscave.admission.dto.response;

import com.thinkerscave.admission.enums.FollowUpType;
import com.thinkerscave.admission.enums.InquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Inquiry record for a prospect")
public class InquiryResponse {

    private Long inquiryId;
    private String name;
    private String mobileNumber;
    private String email;
    private String classInterestedIn;
    private String address;
    private String inquirySource;
    private String referredBy;
    private String comments;
    private Long assignedCounselorId;
    private InquiryStatus status;
    private LocalDateTime lastFollowUpDate;
    private FollowUpType lastFollowUpType;
    private LocalDate nextFollowUpDate;
    private LocalDateTime createdOn;
    private String createdBy;
}
