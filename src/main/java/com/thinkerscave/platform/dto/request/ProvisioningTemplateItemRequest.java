package com.thinkerscave.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProvisioningTemplateItemRequest {

    @NotNull
    private Long templateId;

    @NotBlank
    @Size(max = 50)
    private String itemType;

    @NotBlank
    @Size(max = 150)
    private String itemKey;

    @NotBlank
    @Size(max = 200)
    private String itemName;

    @Size(max = 1000)
    private String itemValue;

    private String configurationJson;

    private Boolean mandatory;

    private Boolean enabled;

    private Integer displayOrder;

    @Size(max = 1000)
    private String remarks;
}
