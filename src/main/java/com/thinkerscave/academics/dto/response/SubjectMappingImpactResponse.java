package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class SubjectMappingImpactResponse {

    private boolean valid;
    private boolean requiresConfirmation;
    private List<Impact> impacts;

    @Getter
    @Setter
    @Builder
    public static class Impact {
        private Long subjectId;
        private String subjectName;
        /** REMOVAL or SCHEDULE_CHANGE. */
        private String type;
        private long teacherAllocationCount;
        private long timetableEntryCount;
        private String message;
    }
}
