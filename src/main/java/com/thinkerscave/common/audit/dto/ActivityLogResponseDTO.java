package com.thinkerscave.common.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogResponseDTO {
    private Long id;
    private Long organizationId;
    private String entityType;
    private Long entityId;
    private String action;
    private String description;
    private String performedBy;
    private Instant performedAt;
}
