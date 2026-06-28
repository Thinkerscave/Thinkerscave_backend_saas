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
        name = "organization_promotions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_org_promotion",
                        columnNames = {"organization_id", "promotion_id"}
                )
        },
        indexes = {
                @Index(name = "idx_org_promotion_org", columnList = "organization_id"),
                @Index(name = "idx_org_promotion_promotion", columnList = "promotion_id"),
                @Index(name = "idx_org_promotion_active", columnList = "active")
        }
)
public class OrganizationPromotion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Organization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /**
     * Promotion.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    /**
     * Applied Discount Percentage.
     */
    @Column(name = "discount_percentage", precision = 5, scale = 2)
    private java.math.BigDecimal discountPercentage;

    /**
     * Applied Discount Amount.
     */
    @Column(name = "discount_amount", precision = 12, scale = 2)
    private java.math.BigDecimal discountAmount;

    /**
     * Final Subscription Amount.
     */
    @Column(name = "final_amount", precision = 12, scale = 2)
    private java.math.BigDecimal finalAmount;

    /**
     * Promotion Applied By.
     */
    @Column(name = "applied_by", length = 100)
    private String appliedBy;

    /**
     * Whether Promotion has been applied.
     */
    @Builder.Default
    @Column(name = "applied")
    private Boolean applied = true;

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