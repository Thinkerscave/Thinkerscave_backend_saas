package com.thinkerscave.common.admission.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Single payload for the Admissions Settings workspace.
 * Avoids the multi-page approach; UI renders one card per section.
 */
@Getter
@Setter
@Builder
public class AdmissionsSettingsResponse {
    private List<OptionItem> inquirySources;
    private List<OptionItem> inquiryStatuses;
    private List<OptionItem> requiredDocuments;
    private AdmissionConfig admissionConfig;
    private CounselorAssignmentRules counselorRules;

    @Getter
    @Setter
    @Builder
    public static class OptionItem {
        private String code;
        private String label;
        private String description;
        private boolean active;
    }

    @Getter
    @Setter
    @Builder
    public static class AdmissionConfig {
        private boolean autoInquiryNumber;
        private boolean autoAdmissionNumber;
        private String admissionNumberPattern;
        private String studentIdPattern;
        private String defaultAdmissionStatus;
    }

    @Getter
    @Setter
    @Builder
    public static class CounselorAssignmentRules {
        private String strategy;
        private boolean balanceWorkload;
        private boolean considerLocation;
    }
}
