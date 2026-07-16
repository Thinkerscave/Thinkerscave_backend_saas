package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileSummaryData {
    private String displayName;
    private String email;
    private String mobileNumber;
    private String roleLabel;
    private String organizationName;
    private String avatarUrl;
}
