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

    /** Organization Setup card — present only for org admin/owner while mandatory setup is incomplete. */
    private Boolean showSetupGuide;
    private Integer setupCompletedCount;
    private Integer setupTotalCount;
    private Integer setupProgressPercent;
    private Boolean setupComplete;
    /** True when the setup status could not be loaded. */
    private Boolean setupError;
}
