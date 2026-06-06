package com.thinkerscave.common.admission.dto;

import java.time.LocalDate;

import com.thinkerscave.common.admission.enums.InquiryStatus;
import lombok.Getter;
import lombok.Setter;

/**
 * Filter payload for the Inquiry Center workspace search.
 * All fields optional; the service combines them with AND semantics.
 */
@Getter
@Setter
public class InquirySearchRequest {
    private String keyword;
    private InquiryStatus status;
    private Long counselorId;
    private String inquirySource;
    private String classInterested;
    private LocalDate followUpFrom;
    private LocalDate followUpTo;
}
