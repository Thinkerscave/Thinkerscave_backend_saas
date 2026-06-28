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
        name = "subscription_feature_overrides",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_org_feature_override",
                        columnNames = {
                                "organization_subscription_id",
                                "feature_id"
                        }
                )
        },
        indexes = {
                @Index(name = "idx_sfo_subscription", columnList = "organization_subscription_id"),
                @Index(name = "idx_sfo_feature", columnList = "feature_id"),
                @Index(name = "idx_sfo_enabled", columnList = "enabled")
        }
)
public class SubscriptionFeatureOverride extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Organization Subscription.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_subscription_id", nullable = false)
    private OrganizationSubscription organizationSubscription;

    /**
     * Feature.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", nullable = false)
    private Feature feature;

    /**
     * Whether feature is enabled for this organization.
     */
    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * Override Reason.
     * Example:
     * Complimentary
     * Custom Requirement
     * Enterprise Deal
     */
    @Column(name = "override_reason", length = 500)
    private String overrideReason;

    /**
     * Override Expiry Date.
     * Null means permanent override.
     */
    @Column(name = "expiry_date")
    private java.time.LocalDate expiryDate;

    /**
     * Complimentary Feature.
     */
    @Builder.Default
    @Column(name = "complimentary")
    private Boolean complimentary = false;

    /**
     * Chargeable Override.
     */
    @Builder.Default
    @Column(name = "chargeable")
    private Boolean chargeable = false;

    /**
     * Additional Charge.
     */
    @Column(name = "additional_charge", precision = 12, scale = 2)
    private java.math.BigDecimal additionalCharge;

    /**
     * Active Flag.
     */
    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Internal Remarks.
     */
    @Column(name = "remarks", length = 1000)
    private String remarks;

}