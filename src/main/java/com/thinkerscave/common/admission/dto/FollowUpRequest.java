package com.thinkerscave.common.admission.dto;

import com.thinkerscave.common.admission.enums.FollowUpType;
import com.thinkerscave.common.admission.enums.InquiryStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class FollowUpRequest {
    private FollowUpType followUpType;
    private String remarks;
    private InquiryStatus statusAfterFollowUp;
    private LocalDate nextFollowUpDate;
}
