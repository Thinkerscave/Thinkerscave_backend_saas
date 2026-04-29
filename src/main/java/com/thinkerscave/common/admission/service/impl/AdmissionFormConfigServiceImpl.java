package com.thinkerscave.common.admission.service.impl;

import org.springframework.transaction.annotation.Transactional;
import com.thinkerscave.common.admission.domain.AdmissionFormTemplate;
import com.thinkerscave.common.admission.dto.AdmissionFormConfigResponse;
import com.thinkerscave.common.admission.dto.AdmissionFormFieldResponse;
import com.thinkerscave.common.admission.repository.AdmissionFormTemplateRepository;
import com.thinkerscave.common.admission.service.AdmissionFormConfigService;
import com.thinkerscave.common.config.TenantContext;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.repository.OrganizationRepository;
import com.thinkerscave.common.shared.enums.OrganizationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class AdmissionFormConfigServiceImpl implements AdmissionFormConfigService {

    private final AdmissionFormTemplateRepository templateRepository;
    private final OrganizationRepository organizationRepository;

    @Override
    public AdmissionFormConfigResponse getFormConfigForCurrentTenant() {
        String tenantId = TenantContext.getTenant();

        return templateRepository.findByTenantIdAndIsActiveTrue(tenantId)
                .map(this::mapToResponse)
                .orElseGet(() -> generateDefaultConfig(tenantId));
    }

    private AdmissionFormConfigResponse mapToResponse(AdmissionFormTemplate template) {
        List<AdmissionFormFieldResponse> fields = template.getFields().stream()
                .map(field -> AdmissionFormFieldResponse.builder()
                        .fieldName(field.getFieldName())
                        .fieldLabel(field.getFieldLabel())
                        .fieldType(field.getFieldType())
                        .options(field.getOptions() != null ? Arrays.asList(field.getOptions().split(",")) : null)
                        .isRequired(field.getIsRequired())
                        .validationPattern(field.getValidationPattern())
                        .fieldOrder(field.getFieldOrder())
                        .stepIndex(field.getStepIndex())
                        .sectionTitle(field.getSectionTitle())
                        .build())
                .collect(Collectors.toList());

        return AdmissionFormConfigResponse.builder()
                .title(template.getTitle())
                .description(template.getDescription())
                .guidelines(template.getGuidelines())
                .fields(fields)
                .build();
    }

    private AdmissionFormConfigResponse generateDefaultConfig(String tenantId) {
        log.info("No admission form template found for tenant: {}. Generating default config.", tenantId);

        OrganizationType orgType = organizationRepository.findByTenantSchema(tenantId)
                .map(Organisation::getType)
                .orElse(OrganizationType.SCHOOL);

        String title = "Admission Form - " + orgType.getDisplayName();
        String guidelines = "Please fill in all the required details accurately to proceed with your application.";

        List<AdmissionFormFieldResponse> fields = new ArrayList<>();

        // Define default fields based on OrgType
        if (orgType == OrganizationType.SCHOOL) {
            fields.add(createField("applying_for_program", "Class", "DROPDOWN",
                    "Class I,Class II,Class III,Class VI,Class IX,Class X,Class XI,Class XII", true, 0, 1));
        } else if (orgType == OrganizationType.NURSING) {
            fields.add(createField("applying_for_program", "Nursing Course", "DROPDOWN",
                    "GNM,B.Sc. Nursing,P.B.B.Sc. Nursing,M.Sc. Nursing,ANM", true, 0, 1));
            fields.add(createField("clinical_experience", "Clinical Work Experience (if any)", "TEXTAREA", null, false,
                    1, 1));
        } else if (orgType == OrganizationType.COLLEGE || orgType == OrganizationType.UNIVERSITY) {
            fields.add(createField("applying_for_program", "Degree Program", "DROPDOWN",
                    "B.A.,B.Sc.,B.Com.,B.Tech,M.A.,M.Sc.,MBA", true, 0, 1));
            fields.add(createField("department", "Department/Faculty", "TEXT", null, true, 1, 1));
            fields.add(createField("major_subject", "Major/Subject", "TEXT", null, true, 2, 1));
        } else if (orgType == OrganizationType.TRAINING_CENTER || orgType == OrganizationType.COACHING_CENTER) {
            fields.add(createField("applying_for_program", "Course/Batch", "DROPDOWN",
                    "Data Science,Web Development,Java Full Stack,Ui/Ux", true, 0, 1));
        }

        return AdmissionFormConfigResponse.builder()
                .title(title)
                .description("Default admission form configuration.")
                .guidelines(guidelines)
                .fields(fields)
                .build();
    }

    private AdmissionFormFieldResponse createField(String name, String label, String type, String options,
            boolean required, int order, int step) {
        return AdmissionFormFieldResponse.builder()
                .fieldName(name)
                .fieldLabel(label)
                .fieldType(type)
                .options(options != null ? Arrays.asList(options.split(",")) : null)
                .isRequired(required)
                .fieldOrder(order)
                .stepIndex(step)
                .build();
    }
}
