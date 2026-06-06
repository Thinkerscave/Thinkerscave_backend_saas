package com.thinkerscave.common.student.workspace.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AchievementResponse {
    private Long achievementId;
    private Long studentId;
    private String category;
    private String title;
    private String description;
    private LocalDate achievementDate;
    private String location;
    private String awardedBy;
    private String icon;
}
