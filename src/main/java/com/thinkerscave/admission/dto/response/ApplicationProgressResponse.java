package com.thinkerscave.admission.dto.response;

import com.thinkerscave.admission.enums.ApplicationStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplicationProgressResponse {

    private Long applicationId;
    private String applicationNumber;
    private ApplicationStatus status;
    private int totalSteps;
    private int completedSteps;
    private int completionPercent;
}