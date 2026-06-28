package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
public class ProvisioningTemplateResponse {

    private Long id;
    private String templateCode;
    private String templateName;
    private String institutionType;
    private String templateVersion;
    private String description;
    private Boolean academicStructureEnabled;
    private Boolean rolesEnabled;
    private Boolean permissionsEnabled;
    private Boolean classesEnabled;
    private Boolean sectionsEnabled;
    private Boolean departmentsEnabled;
    private Boolean designationsEnabled;
    private Boolean seedMasterData;
    private Boolean active;
    private String remarks;
    private List<ProvisioningTemplateItemResponse> items;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
}
