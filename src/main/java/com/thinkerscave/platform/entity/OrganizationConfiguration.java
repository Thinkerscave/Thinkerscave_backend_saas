package com.thinkerscave.platform.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(
        name = "organization_configurations",
        indexes = {
                @Index(name = "idx_org_config_org", columnList = "organization_id")
        }
)
public class OrganizationConfiguration extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Organization.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    /**
     * Default Academic Year.
     */
    @Column(name = "default_academic_year", length = 30)
    private String defaultAcademicYear;

    /**
     * Academic Year Start Month.
     */
    @Column(name = "academic_year_start_month")
    private Integer academicYearStartMonth;

    /**
     * Student Code Pattern.
     * Example:
     * STD-{YEAR}-{SEQ}
     */
    @Column(name = "student_code_pattern", length = 100)
    private String studentCodePattern;

    /**
     * Employee Code Pattern.
     */
    @Column(name = "employee_code_pattern", length = 100)
    private String employeeCodePattern;

    /**
     * Admission Number Pattern.
     */
    @Column(name = "admission_number_pattern", length = 100)
    private String admissionNumberPattern;

    /**
     * Receipt Number Pattern.
     */
    @Column(name = "receipt_number_pattern", length = 100)
    private String receiptNumberPattern;

    /**
     * Invoice Number Pattern.
     */
    @Column(name = "invoice_number_pattern", length = 100)
    private String invoiceNumberPattern;

    /**
     * Default Currency.
     */
    @Column(name = "currency", length = 20)
    private String currency;

    /**
     * Default Time Zone.
     */
    @Column(name = "time_zone", length = 100)
    private String timeZone;

    /**
     * Default Language.
     */
    @Column(name = "language", length = 50)
    private String language;

    /**
     * Date Format.
     */
    @Column(name = "date_format", length = 30)
    private String dateFormat;

    /**
     * Time Format.
     */
    @Column(name = "time_format", length = 20)
    private String timeFormat;

    /**
     * Enable Email Notification.
     */
    @Builder.Default
    @Column(name = "email_notification_enabled")
    private Boolean emailNotificationEnabled = true;

    /**
     * Enable SMS Notification.
     */
    @Builder.Default
    @Column(name = "sms_notification_enabled")
    private Boolean smsNotificationEnabled = false;

    /**
     * Enable WhatsApp Notification.
     */
    @Builder.Default
    @Column(name = "whatsapp_notification_enabled")
    private Boolean whatsappNotificationEnabled = false;

    /**
     * Allow Self Registration.
     */
    @Builder.Default
    @Column(name = "allow_self_registration")
    private Boolean allowSelfRegistration = false;

    /**
     * Multi Branch Enabled.
     */
    @Builder.Default
    @Column(name = "multi_branch_enabled")
    private Boolean multiBranchEnabled = false;

    /**
     * Maintenance Mode.
     */
    @Builder.Default
    @Column(name = "maintenance_mode")
    private Boolean maintenanceMode = false;

    /**
     * Active.
     */
    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;

    /**
     * Remarks.
     */
    @Column(name = "remarks", length = 1000)
    private String remarks;

}