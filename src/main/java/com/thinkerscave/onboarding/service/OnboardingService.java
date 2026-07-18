package com.thinkerscave.onboarding.service;

import com.thinkerscave.onboarding.dto.OnboardingChecklistItemResponse;

import java.util.List;

public interface OnboardingService {

    List<OnboardingChecklistItemResponse> getChecklist();
}
