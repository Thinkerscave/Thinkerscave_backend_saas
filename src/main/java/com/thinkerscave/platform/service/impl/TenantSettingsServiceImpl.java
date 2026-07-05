package com.thinkerscave.platform.service.impl;

import com.thinkerscave.platform.dto.response.TenantConfigResponse;
import com.thinkerscave.platform.entity.Organization;
import com.thinkerscave.platform.enums.InstitutionType;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.service.TenantSettingsService;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TenantSettingsServiceImpl implements TenantSettingsService {

    private final OrganizationRepository organizationRepository;

    @Override
    public TenantConfigResponse getCurrentTenantConfig() {
        Long orgId = OrganizationContext.getOrganizationId();
        if (orgId == null) {
            return schoolDefaults();
        }

        Organization organization = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + orgId));

        return resolveConfig(organization.getInstitutionType());
    }

    private TenantConfigResponse resolveConfig(InstitutionType institutionType) {
        if (institutionType == null) {
            return schoolDefaults();
        }

        return switch (institutionType) {
            case COLLEGE, UNIVERSITY -> TenantConfigResponse.builder()
                    .courseLabel("Program")
                    .containerLabel("Semester")
                    .studentLabel("Student")
                    .allowedContainerTypes(List.of("PROGRAM", "SEMESTER"))
                    .build();
            case COACHING, TRAINING_INSTITUTE -> TenantConfigResponse.builder()
                    .courseLabel("Course")
                    .containerLabel("Batch")
                    .studentLabel("Student")
                    .allowedContainerTypes(List.of("COURSE", "BATCH"))
                    .build();
            default -> schoolDefaults();
        };
    }

    private TenantConfigResponse schoolDefaults() {
        return TenantConfigResponse.builder()
                .courseLabel("Class")
                .containerLabel("Section")
                .studentLabel("Student")
                .allowedContainerTypes(List.of("CLASS", "SECTION"))
                .build();
    }
}
