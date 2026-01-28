package com.thinkerscave.common.admission.dto;

import com.thinkerscave.common.admission.enums.FollowUpType;
import com.thinkerscave.common.admission.enums.InquiryStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class FollowUpResponse {
    private Long id;
    private Long inquiryId;
    private FollowUpType followUpType;
    private String remarks;
    private InquiryStatus statusAfterFollowUp;
    private LocalDateTime followUpDate;
    private LocalDate nextFollowUpDate;
    private String createdBy;
}
