package com.thinkerscave.common.admission.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class InquiryResponse {

    private Long inquiryId;
    private String name;
    private String mobileNumber;
    private String email;
    private String classInterested;
    private String address;
    private String inquirySource;
    private String referredBy;
    private String comments;
    private String assignedCounselor;
    private String status;
    private java.time.LocalDateTime lastFollowUpDate;
    private String lastFollowUpType;
    private java.time.LocalDate nextFollowUpDate;
}

