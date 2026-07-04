package com.thinkerscave.admission.dto.request;

import com.thinkerscave.admission.enums.InquiryStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeadSearchRequest {

    private String keyword;
    private InquiryStatus status;
    private String source;
    private String classInterestedIn;
    private Long counselorId;
    private LocalDate followUpFrom;
    private LocalDate followUpTo;
}