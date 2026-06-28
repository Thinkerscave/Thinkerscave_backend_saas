package com.thinkerscave.platform.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrganizationConfigurationRequest {

    @Size(max = 30)
    private String defaultAcademicYear;

    private Integer academicYearStartMonth;

    @Size(max = 100)
    private String studentCodePattern;

    @Size(max = 100)
    private String employeeCodePattern;

    @Size(max = 100)
    private String admissionNumberPattern;

    @Size(max = 100)
    private String receiptNumberPattern;

    @Size(max = 100)
    private String invoiceNumberPattern;

    @Size(max = 20)
    private String currency;

    @Size(max = 100)
    private String timeZone;

    @Size(max = 50)
    private String language;

    @Size(max = 30)
    private String dateFormat;

    @Size(max = 20)
    private String timeFormat;

    private Boolean emailNotificationEnabled;

    private Boolean smsNotificationEnabled;

    private Boolean whatsappNotificationEnabled;

    private Boolean allowSelfRegistration;

    private Boolean multiBranchEnabled;

    private Boolean maintenanceMode;

    @Size(max = 1000)
    private String remarks;
}
