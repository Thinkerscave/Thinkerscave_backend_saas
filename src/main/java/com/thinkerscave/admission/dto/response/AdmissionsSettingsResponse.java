package com.thinkerscave.admission.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class AdmissionsSettingsResponse {

    private List<String> inquirySources;
    private List<String> inquiryStatuses;
    private List<String> requiredDocuments;
    private Map<String, String> numbering;
    private Map<String, String> reminderRules;
}