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
        name = "subscription_plans",
        indexes = {
                @Index(name = "idx_subscription_plan_code", columnList = "plan_code"),
                @Index(name = "idx_subscription_plan_name", columnList = "plan_name"),
                @Index(name = "idx_subscription_plan_active", columnList = "active")
        }
)
public class SubscriptionPlan extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Business Plan Code.
     * Example:
     * STARTER
     * GROWTH
     * ENTERPRISE
     */
    @Column(name = "plan_code", nullable = false, unique = true, length = 50)
    private String planCode;

    /**
     * Plan Name.
     */
    @Column(name = "plan_name", nullable = false, unique = true, length = 150)
    private String planName;

    /**
     * Description.
     */
    @Column(name = "description", length = 2000)
    private String description;

    /**
     * Monthly Price.
     */
    @Column(name = "monthly_price", precision = 12, scale = 2)
    private java.math.BigDecimal monthlyPrice;

    /**
     * Quarterly Price.
     */
    @Column(name = "quarterly_price", precision = 12, scale = 2)
    private java.math.BigDecimal quarterlyPrice;

    /**
     * Half Yearly Price.
     */
    @Column(name = "half_yearly_price", precision = 12, scale = 2)
    private java.math.BigDecimal halfYearlyPrice;

    /**
     * Yearly Price.
     */
    @Column(name = "yearly_price", precision = 12, scale = 2)
    private java.math.BigDecimal yearlyPrice;

    /**
     * Student Limit.
     * Null = Unlimited
     */
    @Column(name = "student_limit")
    private Integer studentLimit;

    /**
     * Staff Limit.
     */
    @Column(name = "staff_limit")
    private Integer staffLimit;

    /**
     * Branch Limit.
     */
    @Column(name = "branch_limit")
    private Integer branchLimit;

    /**
     * Storage Limit in GB.
     */
    @Column(name = "storage_limit_gb")
    private Integer storageLimitGb;

    /**
     * API Request Limit Per Month.
     */
    @Column(name = "api_request_limit")
    private Long apiRequestLimit;

    /**
     * Trial Days.
     */
    @Builder.Default
    @Column(name = "trial_days")
    private Integer trialDays = 0;

    /**
     * Display Order.
     */
    @Builder.Default
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    /**
     * Highlight Plan.
     */
    @Builder.Default
    @Column(name = "recommended")
    private Boolean recommended = false;

    /**
     * Custom Plan.
     */
    @Builder.Default
    @Column(name = "custom_plan")
    private Boolean customPlan = false;

    /**
     * Visible on Pricing Page.
     */
    @Builder.Default
    @Column(name = "visible")
    private Boolean visible = true;

    /**
     * Active.
     */
    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;

    /**
     * Internal Remarks.
     */
    @Column(name = "remarks", length = 1000)
    private String remarks;

}