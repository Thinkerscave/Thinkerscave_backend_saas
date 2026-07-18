package com.thinkerscave.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.thinkerscave.platform.enums.CustomerStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Lightweight projection for the Customer list page.
 */
@Getter
@Setter
@Builder
public class CustomerListItemResponse {

    private Long id;
    private String customerCode;
    private String customerName;
    /** Future-ready; customers do not store logos today. */
    private String logoUrl;
    /** Derived from business email host when available. */
    private String domain;
    private String ownerName;
    private String ownerEmail;
    private Long organizationCount;
    private CustomerStatus status;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdDate;

    /** Human-readable relative activity, e.g. "2 hours ago". */
    private String lastActivity;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastActivityAt;

    private Boolean active;
}
