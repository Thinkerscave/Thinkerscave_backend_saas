package com.thinkerscave.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationDomainRequest {

    @NotNull
    private Long organizationId;

    @NotBlank
    @Size(max = 150)
    private String subDomain;

    @Size(max = 255)
    private String customDomain;

    private Boolean sslEnabled;

    @Size(max = 1000)
    private String remarks;
}
