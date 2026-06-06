package com.thinkerscave.common.admission.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignCounselorRequest {
    @NotNull
    private Long counselorId;
}
