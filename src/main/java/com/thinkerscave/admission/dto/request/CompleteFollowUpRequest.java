package com.thinkerscave.admission.dto.request;

import com.thinkerscave.admission.enums.InquiryStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CompleteFollowUpRequest {

    private String outcome;
    private String remarks;
    private InquiryStatus statusAfter;
    private LocalDate nextFollowUpDate;
}
