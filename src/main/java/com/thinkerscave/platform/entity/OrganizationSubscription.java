package com.thinkerscave.platform.entity;

import com.thinkerscave.platform.enums.BillingCycle;
import com.thinkerscave.platform.enums.SubscriptionStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(
        name = "organization_subscriptions",
        indexes = {
                @Index(name = "idx_org_subscription_org", columnList = "organization_id"),
                @Index(name = "idx_org_subscription_plan", columnList = "subscription_plan_id"),
                @Index(name = "idx_org_subscription_status", columnList = "status"),
                @Index(name = "idx_org_subscription_expiry", columnList = "end_date")
        }
)
public class OrganizationSubscription extends Auditable {

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
     * Subscription Plan.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    /**
     * Applied Promotion.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    /**
     * Subscription Start Date.
     */
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    /**
     * Subscription End Date.
     */
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    /**
     * Trial End Date.
     */
    @Column(name = "trial_end_date")
    private LocalDate trialEndDate;

    /**
     * Billing Cycle.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 30)
    private BillingCycle billingCycle;

    /**
     * Original Plan Price.
     */
    @Column(name = "plan_price", precision = 12, scale = 2)
    private BigDecimal planPrice;

    /**
     * Discount Amount.
     */
    @Column(name = "discount_amount", precision = 12, scale = 2)
    private BigDecimal discountAmount;

    /**
     * Final Payable Amount.
     */
    @Column(name = "final_amount", precision = 12, scale = 2)
    private BigDecimal finalAmount;

    /**
     * Student Limit Override.
     */
    @Column(name = "student_limit_override")
    private Integer studentLimitOverride;

    /**
     * Staff Limit Override.
     */
    @Column(name = "staff_limit_override")
    private Integer staffLimitOverride;

    /**
     * Branch Limit Override.
     */
    @Column(name = "branch_limit_override")
    private Integer branchLimitOverride;

    /**
     * Storage Limit Override (GB).
     */
    @Column(name = "storage_limit_override")
    private Integer storageLimitOverride;

    /**
     * Auto Renewal Enabled.
     */
    @Builder.Default
    @Column(name = "auto_renew")
    private Boolean autoRenew = false;

    /**
     * Subscription Status.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private SubscriptionStatus status = SubscriptionStatus.ACTIVE;

    /**
     * Active Flag.
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