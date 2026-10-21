package com.thinkerscave.onboarding.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupSubtaskResponse {

    private String key;
    private String label;
    private String description;
    private boolean mandatory;
    private SetupStatus status;
    private String route;
}
