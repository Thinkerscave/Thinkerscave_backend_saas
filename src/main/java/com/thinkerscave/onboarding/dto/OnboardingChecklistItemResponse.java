package com.thinkerscave.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingChecklistItemResponse {

    private String key;
    private String label;
    private boolean completed;
    private long count;
    /** When false, item is shown for awareness but excluded from Phase 1 completion %. */
    private boolean requiredForCompletion;
    /** When false, dependent module is not built yet — do not treat as a failed setup step. */
    private boolean available;
    private String route;
}
