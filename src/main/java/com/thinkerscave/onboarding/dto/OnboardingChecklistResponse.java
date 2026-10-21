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

    private List<SetupCategoryResponse> categories;
    private int completedCount;
    private int totalCount;
    private int progressPercent;
    /** True when every mandatory category is completed. Optional categories do not block this. */
    private boolean setupComplete;
}
