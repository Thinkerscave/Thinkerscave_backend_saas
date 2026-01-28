package com.thinkerscave.common.admission.dto;

import com.thinkerscave.common.admission.enums.InquiryStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class InquirySummaryResponse {
    private Long inquiryId;
    private String name;
    private String mobileNumber;
    private String classInterested;
    private InquiryStatus status;
    private Long assignedCounselorId;
    private LocalDateTime lastFollowUpDate;
    private LocalDate nextFollowUpDate;
    private String comments;
}
