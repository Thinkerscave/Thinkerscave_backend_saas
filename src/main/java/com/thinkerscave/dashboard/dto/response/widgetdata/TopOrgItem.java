package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TopOrgItem {
    private String organizationName;
    private String institutionType;
    private long activeUsers;
    private String planName;
}
