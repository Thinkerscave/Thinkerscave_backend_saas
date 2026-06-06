package com.thinkerscave.common.student.workspace.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AchievementRequest {

    @NotBlank
    private String category;

    @NotBlank
    private String title;

    private String description;
    private LocalDate achievementDate;
    private String location;
    private String awardedBy;
    private String icon;
}
