package com.thinkerscave.admission.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MarkLostRequest {

    @NotBlank
    private String reason;
}