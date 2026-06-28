package com.thinkerscave.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeatureRequest {

    @NotBlank
    @Size(max = 50)
    private String featureCode;

    @NotBlank
    @Size(max = 150)
    private String featureName;

    @Size(max = 150)
    private String displayName;

    @NotBlank
    @Size(max = 100)
    private String module;

    @Size(max = 100)
    private String category;

    private Long parentFeatureId;

    @NotBlank
    @Size(max = 100)
    private String featureKey;

    @Size(max = 2000)
    private String description;

    @Size(max = 100)
    private String icon;

    private Integer displayOrder;

    private Boolean premiumFeature;

    private Boolean visible;

    private Boolean defaultEnabled;

    @Size(max = 1000)
    private String remarks;
}
