package com.thinkerscave.admission.service.impl;

import com.thinkerscave.admission.dto.request.AdmissionsSettingsRequest;
import com.thinkerscave.admission.dto.response.AdmissionsSettingsResponse;
import com.thinkerscave.admission.entity.AdmissionsSetting;
import com.thinkerscave.admission.enums.InquiryStatus;
import com.thinkerscave.admission.enums.LeadSource;
import com.thinkerscave.admission.repository.AdmissionsSettingRepository;
import com.thinkerscave.admission.service.AdmissionsSettingService;
import com.thinkerscave.shared.context.OrganizationContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdmissionsSettingServiceImpl implements AdmissionsSettingService {

    public static final String MODE_MANUAL = "MANUAL";
    public static final String MODE_ROUND_ROBIN = "ROUND_ROBIN";

    private static final List<String> DEFAULT_SOURCES =
            Arrays.stream(LeadSource.values()).map(Enum::name).toList();
    private static final List<String> DEFAULT_STATUSES = Arrays.stream(InquiryStatus.values())
            .map(Enum::name)
            .toList();
    private static final List<String> DEFAULT_DOCS =
            List.of("BIRTH_CERTIFICATE", "AADHAR", "TRANSFER_CERTIFICATE", "PHOTO", "MARKSHEET");

    private final AdmissionsSettingRepository repository;

    @Override
    @Transactional
    public AdmissionsSettingsResponse getSettings() {
        return toResponse(loadOrCreate());
    }

    @Override
    @Transactional
    public AdmissionsSettingsResponse saveSettings(AdmissionsSettingsRequest request) {
        AdmissionsSetting setting = loadOrCreate();
        if (request.getInquirySources() != null) {
            setting.setInquirySources(join(request.getInquirySources()));
        }
        if (request.getInquiryStatuses() != null) {
            setting.setInquiryStatuses(join(request.getInquiryStatuses()));
        }
        if (request.getRequiredDocuments() != null) {
            setting.setRequiredDocuments(join(request.getRequiredDocuments()));
        }
        if (request.getNumbering() != null) {
            if (request.getNumbering().get("leadPrefix") != null) {
                setting.setLeadPrefix(request.getNumbering().get("leadPrefix"));
            }
            if (request.getNumbering().get("applicationPrefix") != null) {
                setting.setApplicationPrefix(request.getNumbering().get("applicationPrefix"));
            }
            if (request.getNumbering().get("admissionPrefix") != null) {
                setting.setAdmissionPrefix(request.getNumbering().get("admissionPrefix"));
            }
        }
        if (request.getReminderRules() != null) {
            if (request.getReminderRules().get("defaultMode") != null) {
                setting.setReminderMode(request.getReminderRules().get("defaultMode"));
            }
            if (request.getReminderRules().get("defaultLeadTime") != null) {
                setting.setReminderLeadTime(request.getReminderRules().get("defaultLeadTime"));
            }
        }
        if (request.getAssignmentMode() != null && !request.getAssignmentMode().isBlank()) {
            setting.setAssignmentMode(normalizeAssignmentMode(request.getAssignmentMode()));
        }
        return toResponse(repository.save(setting));
    }

    @Override
    @Transactional
    public String leadPrefix() {
        return blankToDefault(loadOrCreate().getLeadPrefix(), "LD");
    }

    @Override
    @Transactional
    public String applicationPrefix() {
        return blankToDefault(loadOrCreate().getApplicationPrefix(), "APP");
    }

    @Override
    @Transactional
    public String admissionPrefix() {
        return blankToDefault(loadOrCreate().getAdmissionPrefix(), "ADM");
    }

    @Override
    @Transactional
    public String assignmentMode() {
        return normalizeAssignmentMode(loadOrCreate().getAssignmentMode());
    }

    @Override
    @Transactional
    public boolean isRoundRobinEnabled() {
        return MODE_ROUND_ROBIN.equals(assignmentMode());
    }

    @Override
    @Transactional
    public List<String> requiredDocuments() {
        return split(loadOrCreate().getRequiredDocuments(), DEFAULT_DOCS);
    }

    /**
     * Schema-per-tenant: each school schema has one admissions_setting row.
     * Prefer the current org id when present, otherwise reuse the schema singleton.
     */
    private AdmissionsSetting loadOrCreate() {
        Long orgId = OrganizationContext.getOrganizationId();

        if (orgId != null) {
            var byOrg = repository.findByOrganizationId(orgId);
            if (byOrg.isPresent()) {
                return byOrg.get();
            }
        }

        var existing = repository.findFirstByOrderBySettingIdAsc();
        if (existing.isPresent()) {
            AdmissionsSetting setting = existing.get();
            if (orgId != null && !orgId.equals(setting.getOrganizationId())) {
                setting.setOrganizationId(orgId);
                return repository.save(setting);
            }
            return setting;
        }

        AdmissionsSetting created = new AdmissionsSetting();
        created.setOrganizationId(orgId != null ? orgId : 0L);
        created.setInquirySources(join(DEFAULT_SOURCES));
        created.setInquiryStatuses(join(DEFAULT_STATUSES));
        created.setRequiredDocuments(join(DEFAULT_DOCS));
        created.setLeadPrefix("LD");
        created.setApplicationPrefix("APP");
        created.setAdmissionPrefix("ADM");
        created.setReminderMode("AUTO");
        created.setReminderLeadTime("24H");
        created.setAssignmentMode(MODE_MANUAL);
        created.setNextCounselorIndex(0L);
        return repository.save(created);
    }

    private AdmissionsSettingsResponse toResponse(AdmissionsSetting setting) {
        Map<String, String> numbering = new LinkedHashMap<>();
        numbering.put("leadPrefix", blankToDefault(setting.getLeadPrefix(), "LD"));
        numbering.put("applicationPrefix", blankToDefault(setting.getApplicationPrefix(), "APP"));
        numbering.put("admissionPrefix", blankToDefault(setting.getAdmissionPrefix(), "ADM"));

        Map<String, String> reminders = new LinkedHashMap<>();
        reminders.put("defaultMode", blankToDefault(setting.getReminderMode(), "AUTO"));
        reminders.put("defaultLeadTime", blankToDefault(setting.getReminderLeadTime(), "24H"));

        return AdmissionsSettingsResponse.builder()
                .inquirySources(split(setting.getInquirySources(), DEFAULT_SOURCES))
                .inquiryStatuses(split(setting.getInquiryStatuses(), DEFAULT_STATUSES))
                .requiredDocuments(split(setting.getRequiredDocuments(), DEFAULT_DOCS))
                .numbering(numbering)
                .reminderRules(reminders)
                .assignmentMode(normalizeAssignmentMode(setting.getAssignmentMode()))
                .build();
    }

    static String normalizeAssignmentMode(String raw) {
        if (raw == null || raw.isBlank()) {
            return MODE_MANUAL;
        }
        String mode = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if ("AUTO".equals(mode) || "AUTOMATIC".equals(mode) || "ROUNDROBIN".equals(mode) || "ROUND_ROBIN".equals(mode)) {
            return MODE_ROUND_ROBIN;
        }
        return MODE_MANUAL;
    }

    private static String join(List<String> values) {
        return values.stream()
                .filter(v -> v != null && !v.isBlank())
                .map(String::trim)
                .collect(Collectors.joining("|"));
    }

    private static List<String> split(String raw, List<String> fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        return Arrays.stream(raw.split("\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
