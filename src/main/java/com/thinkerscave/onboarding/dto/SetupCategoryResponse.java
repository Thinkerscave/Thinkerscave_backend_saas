package com.thinkerscave.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupCategoryResponse {

    private String key;
    private String label;
    private String description;
    private boolean mandatory;
    private SetupStatus status;
    private String route;
    private List<SetupSubtaskResponse> subtasks;
}
