package com.thinkerscave.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingChecklistResponse {

    private List<OnboardingChecklistItemResponse> items;
    private int completedRequiredCount;
    private int requiredCount;
    private int progressPercent;
    private String recommendedNextKey;
    private String recommendedNextLabel;
    private String recommendedNextRoute;
    private boolean setupComplete;
}
