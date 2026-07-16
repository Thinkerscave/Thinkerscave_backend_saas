package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WelcomeHeaderData {
    private String displayName;
    private String roleLabel;
    private String organizationName;
    private String greeting;
    private String avatarUrl;
    private String todayLabel;
}
