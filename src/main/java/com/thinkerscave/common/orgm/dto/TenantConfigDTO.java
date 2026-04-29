package com.thinkerscave.common.orgm.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantConfigDTO {
    private String courseLabel;
    private String containerLabel;
    private String studentLabel;
    private List<String> allowedContainerTypes;
}
