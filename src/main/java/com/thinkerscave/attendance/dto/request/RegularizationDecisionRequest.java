package com.thinkerscave.attendance.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Approve or reject a regularization request")
public class RegularizationDecisionRequest {

    @NotBlank(message = "Comment is required")
    private String comment;
}
