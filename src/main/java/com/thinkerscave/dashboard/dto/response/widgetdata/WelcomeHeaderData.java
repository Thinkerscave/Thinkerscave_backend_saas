package com.thinkerscave.dashboard.dto.response.widgetdata;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WelcomeHeaderData {
    private String displayName;
    private String roleLabel;
    private String organizationName;
    private String greeting;
    private String avatarUrl;
    private String todayLabel;

    /** Guided setup — present only for org admin/owner while setup is incomplete. */
    private Boolean showSetupGuide;
    private Integer setupProgressPercent;
    private String recommendedNextLabel;
    private String recommendedNextRoute;
    private List<SetupChecklistItem> setupChecklist;
    private Boolean setupComplete;

    @Data
    @Builder
    public static class SetupChecklistItem {
        private String key;
        private String label;
        private boolean completed;
        private boolean requiredForCompletion;
        private boolean available;
        private String route;
    }
}
