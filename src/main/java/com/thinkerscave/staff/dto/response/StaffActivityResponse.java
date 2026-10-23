package com.thinkerscave.staff.dto.response;

import com.thinkerscave.staff.enums.StaffActivityType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StaffActivityResponse {

    private Long activityId;
    private StaffActivityType actionType;
    private String section;
    private String description;
    private String performedBy;
    private LocalDateTime performedOn;
}
