package com.thinkerscave.academics.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TeacherCandidateResponse {
    private Long staffId;
    private String name;
    private String staffCode;
    private String primarySubjectName;
    private String secondarySubjectName;
    /** 1 = primary subject match, 2 = secondary subject match, 3 = other eligible teacher. */
    private int preferenceRank;
    private int currentPeriods;
    private int projectedPeriods;
    private int maxPeriods;
    /** AVAILABLE, LIMITED, AT_CAPACITY, or EXCEEDS_LIMIT once the assignment is added. */
    private String workloadStatus;
}
