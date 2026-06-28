package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class FeatureResponse {

    private Long id;
    private String featureCode;
    private String featureName;
    private String displayName;
    private String module;
    private String category;
    private Long parentFeatureId;
    private String parentFeatureName;
    private String featureKey;
    private String description;
    private String icon;
    private Integer displayOrder;
    private Boolean premiumFeature;
    private Boolean visible;
    private Boolean defaultEnabled;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
}
