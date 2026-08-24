package com.thinkerscave.admission.service;

import com.thinkerscave.admission.dto.request.AdmissionsSettingsRequest;
import com.thinkerscave.admission.dto.response.AdmissionsSettingsResponse;

public interface AdmissionsSettingService {

    AdmissionsSettingsResponse getSettings();

    AdmissionsSettingsResponse saveSettings(AdmissionsSettingsRequest request);

    String leadPrefix();

    String applicationPrefix();

    String admissionPrefix();

    java.util.List<String> requiredDocuments();
}
