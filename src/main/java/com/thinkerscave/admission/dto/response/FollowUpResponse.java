package com.thinkerscave.admission.dto.response;

import com.thinkerscave.admission.enums.FollowUpType;
import com.thinkerscave.admission.enums.InquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "Follow-up interaction record")
public class FollowUpResponse {

    private Long followUpId;
    private Long inquiryId;
    private FollowUpType followUpType;
    private String remarks;
    private InquiryStatus statusAfter;
    private LocalDateTime followUpDate;
    private LocalDate nextFollowUpDate;
    private LocalDateTime createdOn;
    private String createdBy;
}
