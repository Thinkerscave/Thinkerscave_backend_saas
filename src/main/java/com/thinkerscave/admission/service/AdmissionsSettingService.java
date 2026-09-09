package com.thinkerscave.admission.service;

import com.thinkerscave.admission.dto.request.AdmissionsSettingsRequest;
import com.thinkerscave.admission.dto.response.AdmissionsSettingsResponse;

public interface AdmissionsSettingService {

    AdmissionsSettingsResponse getSettings();

    AdmissionsSettingsResponse saveSettings(AdmissionsSettingsRequest request);

    String leadPrefix();

    String applicationPrefix();

    String admissionPrefix();

    /** "MANUAL" or "ROUND_ROBIN" — controls whether new leads are auto-assigned a counselor. */
    String assignmentMode();

    java.util.List<String> requiredDocuments();
}
