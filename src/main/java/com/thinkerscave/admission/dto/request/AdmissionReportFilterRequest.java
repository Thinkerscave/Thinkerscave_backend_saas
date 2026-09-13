package com.thinkerscave.admission.dto.request;

import com.thinkerscave.admission.enums.ApplicationStatus;
import com.thinkerscave.admission.enums.InquiryStatus;
import com.thinkerscave.admission.enums.LeadSource;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Shared filters for the admissions management report.
 * All calculations for a request must use the same filter set.
 */
@Getter
@Setter
public class AdmissionReportFilterRequest {

    private Long academicYearId;
    private Long classId;
    private LeadSource source;
    private Long counselorId;
    private InquiryStatus leadStatus;
    private ApplicationStatus applicationStatus;
    /** Inclusive created-on range (inquiry / application createdOn). */
    private LocalDate dateFrom;
    private LocalDate dateTo;
    /** WEEKLY or MONTHLY trend buckets. Default MONTHLY. */
    private String trendGranularity = "MONTHLY";
    /** Recent applications page size (capped server-side). */
    private Integer recentLimit;
}
