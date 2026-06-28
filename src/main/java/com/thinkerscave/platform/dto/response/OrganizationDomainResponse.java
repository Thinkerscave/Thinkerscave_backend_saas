package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.DomainStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class OrganizationDomainResponse {

    private Long id;
    private Long organizationId;
    private String organizationName;
    private String subDomain;
    private String domain;
    private String customDomain;
    private Boolean sslEnabled;
    private String sslProvider;
    private LocalDate sslExpiry;
    private Boolean dnsVerified;
    private String verificationToken;
    private Boolean defaultDomain;
    private Boolean primaryDomain;
    private DomainStatus status;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
}
