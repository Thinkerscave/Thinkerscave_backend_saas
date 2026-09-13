package com.thinkerscave.admission.dto.request;

import com.thinkerscave.admission.enums.FollowUpType;
import com.thinkerscave.admission.enums.InquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "Schedule a planned follow-up for an inquiry (not a completed interaction)")
public class FollowUpRequest {

    @NotNull(message = "Follow-up type is required")
    private FollowUpType followUpType;

    private String remarks;

    @Schema(description = "Deprecated for schedule flow — status changes happen via counseling notes")
    private InquiryStatus statusAfter;

    @Schema(description = "When the planned follow-up should occur")
    private LocalDateTime followUpDate;

    @Schema(description = "Deprecated for schedule flow — use followUpDate as the planned slot")
    private LocalDate nextFollowUpDate;
}
