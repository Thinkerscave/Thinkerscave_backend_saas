package com.thinkerscave.admission.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class InquiryTimelineItemResponse {

    private String eventType;
    private String title;
    private String description;
    private String performedBy;
    private LocalDateTime performedOn;
}