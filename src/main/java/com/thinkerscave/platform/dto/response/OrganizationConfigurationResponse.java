package com.thinkerscave.platform.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class OrganizationConfigurationResponse {

    private Long id;
    private Long organizationId;
    private String defaultAcademicYear;
    private Integer academicYearStartMonth;
    private String studentCodePattern;
    private String employeeCodePattern;
    private String admissionNumberPattern;
    private String receiptNumberPattern;
    private String invoiceNumberPattern;
    private String currency;
    private String timeZone;
    private String language;
    private String dateFormat;
    private String timeFormat;
    private Boolean emailNotificationEnabled;
    private Boolean smsNotificationEnabled;
    private Boolean whatsappNotificationEnabled;
    private Boolean allowSelfRegistration;
    private Boolean multiBranchEnabled;
    private Boolean maintenanceMode;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
}
