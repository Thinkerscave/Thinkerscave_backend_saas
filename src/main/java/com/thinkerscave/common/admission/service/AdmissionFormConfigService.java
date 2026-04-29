package com.thinkerscave.common.admission.service;

import com.thinkerscave.common.admission.dto.AdmissionFormConfigResponse;

public interface AdmissionFormConfigService {
    AdmissionFormConfigResponse getFormConfigForCurrentTenant();
}
