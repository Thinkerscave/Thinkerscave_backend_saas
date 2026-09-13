package com.thinkerscave.admission.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.thinkerscave.admission.enums.InquiryStatus;
import com.thinkerscave.admission.enums.LeadSource;
import lombok.Data;

import java.time.LocalDate;

@Data
public class LeadSearchRequest {

    private String keyword;
    private InquiryStatus status;
    @JsonAlias({"inquirySource", "source"})
    private LeadSource source;
    @JsonAlias({"classInterested", "classInterestedIn"})
    private String classInterestedIn;
    private Long academicYearId;
    private Long classId;
    private Long counselorId;
    private LocalDate followUpFrom;
    private LocalDate followUpTo;
    /** MY = created by current user; ALL = all tenant leads the caller can view. */
    @JsonAlias({"leadScope", "scope"})
    private String scope;
    /** Set by service for MY scope — matches Auditable.createdBy (username). */
    private String createdBy;
}