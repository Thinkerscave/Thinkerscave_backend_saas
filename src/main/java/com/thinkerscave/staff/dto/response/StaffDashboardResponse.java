package com.thinkerscave.staff.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StaffDashboardResponse {

    private long totalStaff;
    private long teachingStaff;
    private long nonTeachingStaff;
    private long activeStaff;
    private long temporaryStaff;
    private long contractStaff;
}
