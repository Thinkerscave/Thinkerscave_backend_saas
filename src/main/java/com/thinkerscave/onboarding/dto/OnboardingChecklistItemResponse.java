package com.thinkerscave.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class OnboardingChecklistItemResponse {

    private String key;
    private String label;
    private boolean completed;
    private long count;
}
