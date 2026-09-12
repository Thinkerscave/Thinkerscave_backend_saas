package com.thinkerscave.admission.service;

import com.thinkerscave.admission.dto.request.AdmissionsSettingsRequest;
import com.thinkerscave.admission.dto.response.AdmissionsSettingsResponse;

public interface AdmissionsSettingService {

    AdmissionsSettingsResponse getSettings();

    AdmissionsSettingsResponse saveSettings(AdmissionsSettingsRequest request);

    String leadPrefix();

    String applicationPrefix();

    String admissionPrefix();

    /** Normalized assignment mode: {@code MANUAL} or {@code ROUND_ROBIN}. */
    String assignmentMode();

    /** True when new leads should be auto-assigned via sequential round-robin. */
    boolean isRoundRobinEnabled();

    java.util.List<String> requiredDocuments();
}
