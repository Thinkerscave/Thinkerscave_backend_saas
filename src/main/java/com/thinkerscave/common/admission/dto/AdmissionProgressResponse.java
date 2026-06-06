package com.thinkerscave.common.admission.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class AdmissionProgressResponse {
    private String applicationId;
    private int currentStep;
    private int totalSteps;
    private int progressPercentage;
    private List<Integer> completedSteps;
    private List<String> pendingFields;
    private String status;
}
