package com.thinkerscave.common.orgm.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.thinkerscave.common.course.enums.ContainerType;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.dto.TenantConfigDTO;
import com.thinkerscave.common.orgm.repository.OrganizationRepository;
import com.thinkerscave.common.shared.enums.OrganizationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantSettingsService {

    private final OrganizationRepository organisationRepository;
    private final ObjectMapper objectMapper;

    /**
     * Gets the TenantConfigDTO for a specific organization by schema.
     * If tenantSettings is null or invalid in DB, returns defaults based on
     * OrganizationType.
     */
    public TenantConfigDTO getTenantConfigBySchema(String tenantSchema) {
        Optional<Organisation> orgOptional = organisationRepository.findByTenantSchema(tenantSchema);

        if (orgOptional.isEmpty()) {
            return getDefaultConfig(OrganizationType.SCHOOL); // Fallback
        }

        Organisation org = orgOptional.get();

        if (org.getTenantSettings() != null && !org.getTenantSettings().trim().isEmpty()) {
            try {
                return objectMapper.readValue(org.getTenantSettings(), TenantConfigDTO.class);
            } catch (JsonProcessingException e) {
                log.error("Failed to parse tenant_settings for tenantSchema: {}", tenantSchema, e);
                // Fallback to default if parsing fails
            }
        }

        return getDefaultConfig(org.getType() != null ? org.getType() : OrganizationType.SCHOOL);
    }

    private TenantConfigDTO getDefaultConfig(OrganizationType type) {
        switch (type) {
            case NURSING:
                return TenantConfigDTO.builder()
                        .courseLabel("Nursing Course")
                        .containerLabel("Batch")
                        .studentLabel("Student")
                        .allowedContainerTypes(List.of(
                                ContainerType.BATCH.name(),
                                ContainerType.YEAR.name(),
                                ContainerType.SECTION.name()))
                        .build();
            case COLLEGE:
            case UNIVERSITY:
            case GROUP:
            case INSTITUTION:
                return TenantConfigDTO.builder()
                        .courseLabel("Degree Program")
                        .containerLabel("Batch")
                        .studentLabel("Student")
                        .allowedContainerTypes(List.of(
                                ContainerType.DEPARTMENT.name(),
                                ContainerType.BRANCH.name(),
                                ContainerType.YEAR.name(),
                                ContainerType.BATCH.name(),
                                ContainerType.SECTION.name()))
                        .build();
            case TRAINING_CENTER:
            case COACHING_CENTER:
                return TenantConfigDTO.builder()
                        .courseLabel("Course")
                        .containerLabel("Batch")
                        .studentLabel("Student")
                        .allowedContainerTypes(List.of(
                                ContainerType.MODULE.name(),
                                ContainerType.BATCH.name()))
                        .build();
            case SCHOOL:
            default:
                return TenantConfigDTO.builder()
                        .courseLabel("Class")
                        .containerLabel("Section")
                        .studentLabel("Student")
                        .allowedContainerTypes(List.of(
                                ContainerType.CLASS.name(),
                                ContainerType.SECTION.name()))
                        .build();
        }
    }
}
