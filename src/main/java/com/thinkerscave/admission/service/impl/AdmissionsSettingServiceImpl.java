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

import java.util.ArrayList;
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
        if (request.getRequiredDocuments() != null || request.getOptionalDocuments() != null) {
            List<String> required = request.getRequiredDocuments() != null
                    ? request.getRequiredDocuments()
                    : parseDocumentBundle(setting.getRequiredDocuments()).required();
            List<String> optional = request.getOptionalDocuments() != null
                    ? request.getOptionalDocuments()
                    : parseDocumentBundle(setting.getRequiredDocuments()).optional();
            setting.setRequiredDocuments(encodeDocumentBundle(required, optional));
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
            if (request.getReminderRules().get("leadIdleDays") != null) {
                setting.setReminderMode(normalizeDays(request.getReminderRules().get("leadIdleDays"), "3"));
            } else if (request.getReminderRules().get("defaultMode") != null) {
                setting.setReminderMode(request.getReminderRules().get("defaultMode"));
            }
            if (request.getReminderRules().get("missedFollowUpDays") != null) {
                setting.setReminderLeadTime(normalizeDays(request.getReminderRules().get("missedFollowUpDays"), "2"));
            } else if (request.getReminderRules().get("defaultLeadTime") != null) {
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
        return parseDocumentBundle(loadOrCreate().getRequiredDocuments()).required();
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
        created.setRequiredDocuments(encodeDocumentBundle(DEFAULT_DOCS, List.of()));
        created.setLeadPrefix("LD");
        created.setApplicationPrefix("APP");
        created.setAdmissionPrefix("ADM");
        created.setReminderMode("3");
        created.setReminderLeadTime("2");
        created.setAssignmentMode(MODE_MANUAL);
        created.setNextCounselorIndex(0L);
        return repository.save(created);
    }

    private AdmissionsSettingsResponse toResponse(AdmissionsSetting setting) {
        Map<String, String> numbering = new LinkedHashMap<>();
        numbering.put("leadPrefix", blankToDefault(setting.getLeadPrefix(), "LD"));
        numbering.put("applicationPrefix", blankToDefault(setting.getApplicationPrefix(), "APP"));
        numbering.put("admissionPrefix", blankToDefault(setting.getAdmissionPrefix(), "ADM"));

        DocumentBundle docs = parseDocumentBundle(setting.getRequiredDocuments());
        Map<String, String> reminders = new LinkedHashMap<>();
        reminders.put("leadIdleDays", parseStoredDays(setting.getReminderMode(), "3"));
        reminders.put("missedFollowUpDays", parseStoredDays(setting.getReminderLeadTime(), "2"));
        // Keep legacy keys for older clients.
        reminders.put("defaultMode", "AUTO");
        reminders.put("defaultLeadTime", "24H");

        return AdmissionsSettingsResponse.builder()
                .inquirySources(split(setting.getInquirySources(), DEFAULT_SOURCES))
                .inquiryStatuses(split(setting.getInquiryStatuses(), DEFAULT_STATUSES))
                .requiredDocuments(docs.required())
                .optionalDocuments(docs.optional())
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

    private static String encodeDocumentBundle(List<String> required, List<String> optional) {
        List<String> parts = new ArrayList<>();
        if (required != null) {
            for (String type : required) {
                String normalized = normalizeDoc(type);
                if (!normalized.isEmpty()) {
                    parts.add("M:" + normalized);
                }
            }
        }
        if (optional != null) {
            for (String type : optional) {
                String normalized = normalizeDoc(type);
                if (!normalized.isEmpty()) {
                    parts.add("O:" + normalized);
                }
            }
        }
        return String.join("|", parts);
    }

    private static DocumentBundle parseDocumentBundle(String raw) {
        if (raw == null || raw.isBlank()) {
            return new DocumentBundle(DEFAULT_DOCS, List.of());
        }
        List<String> required = new ArrayList<>();
        List<String> optional = new ArrayList<>();
        for (String part : raw.split("\\|")) {
            String token = part == null ? "" : part.trim();
            if (token.isEmpty()) {
                continue;
            }
            if (token.regionMatches(true, 0, "O:", 0, 2)) {
                String type = normalizeDoc(token.substring(2));
                if (!type.isEmpty() && !optional.contains(type) && !required.contains(type)) {
                    optional.add(type);
                }
            } else if (token.regionMatches(true, 0, "M:", 0, 2)) {
                String type = normalizeDoc(token.substring(2));
                if (!type.isEmpty() && !required.contains(type)) {
                    required.add(type);
                    optional.remove(type);
                }
            } else {
                String type = normalizeDoc(token);
                if (!type.isEmpty() && !required.contains(type)) {
                    required.add(type);
                }
            }
        }
        if (required.isEmpty() && optional.isEmpty()) {
            return new DocumentBundle(DEFAULT_DOCS, List.of());
        }
        return new DocumentBundle(List.copyOf(required), List.copyOf(optional));
    }

    private static String normalizeDoc(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT).replaceAll("\\s+", "_");
    }

    private static String normalizeDays(String raw, String fallback) {
        try {
            int days = Integer.parseInt(raw == null ? "" : raw.trim().replaceAll("[^0-9]", ""));
            if (days < 1) {
                return fallback;
            }
            if (days > 30) {
                return "30";
            }
            return String.valueOf(days);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String parseStoredDays(String raw, String fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        String trimmed = raw.trim();
        if ("AUTO".equalsIgnoreCase(trimmed) || "MANUAL".equalsIgnoreCase(trimmed)
                || trimmed.toUpperCase(Locale.ROOT).endsWith("H")) {
            return fallback;
        }
        return normalizeDays(trimmed, fallback);
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

    private record DocumentBundle(List<String> required, List<String> optional) {
    }
}
