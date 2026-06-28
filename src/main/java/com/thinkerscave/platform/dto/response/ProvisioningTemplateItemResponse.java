package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class ProvisioningTemplateItemResponse {

    private Long id;
    private Long templateId;
    private String itemType;
    private String itemKey;
    private String itemName;
    private String itemValue;
    private String configurationJson;
    private Boolean mandatory;
    private Boolean enabled;
    private Integer displayOrder;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdOn;
}
