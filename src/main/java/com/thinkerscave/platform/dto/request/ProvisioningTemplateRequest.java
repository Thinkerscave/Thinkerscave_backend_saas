package com.thinkerscave.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProvisioningTemplateRequest {

    @NotBlank
    @Size(max = 50)
    private String templateCode;

    @NotBlank
    @Size(max = 150)
    private String templateName;

    @NotBlank
    @Size(max = 100)
    private String institutionType;

    @NotBlank
    @Size(max = 20)
    private String templateVersion;

    @Size(max = 2000)
    private String description;

    private Boolean academicStructureEnabled;

    private Boolean rolesEnabled;

    private Boolean permissionsEnabled;

    private Boolean classesEnabled;

    private Boolean sectionsEnabled;

    private Boolean departmentsEnabled;

    private Boolean designationsEnabled;

    private Boolean seedMasterData;

    @Size(max = 1000)
    private String remarks;
}
