package com.thinkerscave.finance.controller;

import com.thinkerscave.finance.dto.request.GenerationRunRequest;
import com.thinkerscave.finance.dto.request.GenerationSettingsRequest;
import com.thinkerscave.finance.dto.request.ReminderSettingsRequest;
import com.thinkerscave.finance.dto.response.FinanceSettingsResponse;
import com.thinkerscave.finance.security.FinanceAccessGuard;
import com.thinkerscave.finance.service.FinanceSettingsService;
import com.thinkerscave.shared.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/fees/settings")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FinanceSettingsController {

    private final FinanceSettingsService financeSettingsService;
    private final FinanceAccessGuard accessGuard;

    @GetMapping
    public ResponseEntity<ApiResponse<FinanceSettingsResponse>> get() {
        accessGuard.requireView(FinanceAccessGuard.RESOURCE_SETTINGS);
        return ResponseEntity.ok(ApiResponse.success("Finance settings", financeSettingsService.get()));
    }

    @PutMapping("/generation")
    public ResponseEntity<ApiResponse<FinanceSettingsResponse>> updateGeneration(
            @Valid @RequestBody GenerationSettingsRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_SETTINGS);
        return ResponseEntity.ok(ApiResponse.success("Generation settings updated",
                financeSettingsService.updateGeneration(request)));
    }

    @PutMapping("/reminders")
    public ResponseEntity<ApiResponse<FinanceSettingsResponse>> updateReminders(
            @Valid @RequestBody ReminderSettingsRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_SETTINGS);
        return ResponseEntity.ok(ApiResponse.success("Reminder settings updated",
                financeSettingsService.updateReminders(request)));
    }

    @PostMapping("/generation/run")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runGeneration(
            @Valid @RequestBody GenerationRunRequest request) {
        accessGuard.requireManage(FinanceAccessGuard.RESOURCE_SETTINGS);
        int created = financeSettingsService.runGeneration(request);
        return ResponseEntity.ok(ApiResponse.success("Generation run complete", Map.of("createdPeriods", created)));
    }
}
