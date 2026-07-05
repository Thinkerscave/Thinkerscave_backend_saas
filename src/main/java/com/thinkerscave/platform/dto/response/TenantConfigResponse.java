package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class TenantConfigResponse {

    private String courseLabel;
    private String containerLabel;
    private String studentLabel;
    private List<String> allowedContainerTypes;
}
