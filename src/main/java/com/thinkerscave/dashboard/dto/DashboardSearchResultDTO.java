package com.thinkerscave.dashboard.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardSearchResultDTO {

    private String key;
    private String entityType;
    private String entityId;
    private String title;
    private String subtitle;
    private String detail;
    private String icon;
    private String route;
    private String tone;
}
