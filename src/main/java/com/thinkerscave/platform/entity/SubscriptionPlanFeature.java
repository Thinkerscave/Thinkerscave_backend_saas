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
        name = "subscription_plan_features",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_subscription_feature",
                        columnNames = {"subscription_plan_id", "feature_id"}
                )
        },
        indexes = {
                @Index(name = "idx_spf_plan", columnList = "subscription_plan_id"),
                @Index(name = "idx_spf_feature", columnList = "feature_id"),
                @Index(name = "idx_spf_active", columnList = "active")
        }
)
public class SubscriptionPlanFeature extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Subscription Plan.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id", nullable = false)
    private SubscriptionPlan subscriptionPlan;

    /**
     * Platform Feature.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feature_id", nullable = false)
    private Feature feature;

    /**
     * Feature Enabled in this Plan.
     */
    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    /**
     * Mandatory Feature.
     * Mandatory features cannot be disabled through feature override.
     */
    @Builder.Default
    @Column(name = "mandatory", nullable = false)
    private Boolean mandatory = false;

    /**
     * Display Order.
     */
    @Builder.Default
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    /**
     * Feature Notes.
     */
    @Column(name = "notes", length = 1000)
    private String notes;

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