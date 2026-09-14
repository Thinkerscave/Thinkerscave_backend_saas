package com.thinkerscave.finance.service;

import com.thinkerscave.finance.dto.request.GenerationRunRequest;
import com.thinkerscave.finance.dto.request.GenerationSettingsRequest;
import com.thinkerscave.finance.dto.request.ReminderSettingsRequest;
import com.thinkerscave.finance.dto.response.FinanceSettingsResponse;

public interface FinanceSettingsService {
    FinanceSettingsResponse get();
    FinanceSettingsResponse updateGeneration(GenerationSettingsRequest request);
    FinanceSettingsResponse updateReminders(ReminderSettingsRequest request);
    int runGeneration(GenerationRunRequest request);
}
