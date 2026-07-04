package com.thinkerscave.admission.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignCounselorRequest {

    @NotNull
    private Long counselorId;
}