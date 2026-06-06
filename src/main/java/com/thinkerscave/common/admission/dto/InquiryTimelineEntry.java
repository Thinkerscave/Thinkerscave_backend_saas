package com.thinkerscave.common.admission.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Immutable timeline entry rendered on the Inquiry Detail Timeline tab.
 */
@Getter
@Setter
@Builder
public class InquiryTimelineEntry {
    private String action;
    private String description;
    private String performedBy;
    private Instant performedAt;
    private String icon;
    private String tone;
}
