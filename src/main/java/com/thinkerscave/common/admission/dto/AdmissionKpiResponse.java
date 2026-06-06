package com.thinkerscave.common.admission.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * KPI summary for the Admission Center workspace.
 * Maps existing ApplicationStatus enum values to spec-defined buckets.
 */
@Getter
@Setter
@Builder
public class AdmissionKpiResponse {
    private long inProgress;
    private long documentsPending;
    private long verificationPending;
    private long readyToEnroll;
    private long completed;
}
