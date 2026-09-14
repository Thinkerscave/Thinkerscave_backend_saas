package com.thinkerscave.finance.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClonePreviewResponse {
    private List<CloneClassPreview> classes;

    @Data
    @Builder
    public static class CloneClassPreview {
        private Long classId;
        private String proposedName;
        private Short dueDay;
        private Conflict conflict;
    }

    @Data
    @Builder
    public static class Conflict {
        private Long classId;
        private Long existingStructureId;
        private String existingName;
    }
}
