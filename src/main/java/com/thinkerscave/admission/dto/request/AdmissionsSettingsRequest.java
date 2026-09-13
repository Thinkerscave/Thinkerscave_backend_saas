package com.thinkerscave.admission.dto.request;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class AdmissionsSettingsRequest {

    private List<String> inquirySources;
    private List<String> inquiryStatuses;
    private List<String> requiredDocuments;
    private List<String> optionalDocuments;
    private Map<String, String> numbering;
    private Map<String, String> reminderRules;
    private String assignmentMode;
}
