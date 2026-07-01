package com.thinkerscave.admission.dto.request;

import com.thinkerscave.admission.enums.FollowUpType;
import com.thinkerscave.admission.enums.InquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "Record a follow-up interaction for an inquiry")
public class FollowUpRequest {

    @NotNull(message = "Follow-up type is required")
    private FollowUpType followUpType;

    private String remarks;

    @Schema(description = "Updated status after this interaction")
    private InquiryStatus statusAfter;

    private LocalDateTime followUpDate;

    private LocalDate nextFollowUpDate;
}
