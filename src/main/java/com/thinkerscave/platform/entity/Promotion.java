package com.thinkerscave.platform.entity;

import com.thinkerscave.platform.enums.DiscountType;
import com.thinkerscave.platform.enums.PromotionStatus;
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
        name = "promotions",
        indexes = {
                @Index(name = "idx_promotion_code", columnList = "promotion_code"),
                @Index(name = "idx_promotion_name", columnList = "promotion_name"),
                @Index(name = "idx_promotion_status", columnList = "status")
        }
)
public class Promotion extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Business Promotion Code.
     * Example :
     * SUMMER2026
     * FOUNDER30
     */
    @Column(name = "promotion_code", nullable = false, unique = true, length = 50)
    private String promotionCode;

    /**
     * Promotion Name.
     */
    @Column(name = "promotion_name", nullable = false, length = 150)
    private String promotionName;

    /**
     * Description.
     */
    @Column(name = "description", length = 2000)
    private String description;

    /**
     * Discount Type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 30)
    private DiscountType discountType;

    /**
     * Discount Value.
     * Percentage or Flat Amount.
     */
    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountValue;

    /**
     * Maximum Discount Amount.
     */
    @Column(name = "maximum_discount", precision = 12, scale = 2)
    private BigDecimal maximumDiscount;

    /**
     * Valid From.
     */
    @Column(name = "valid_from")
    private LocalDate validFrom;

    /**
     * Valid Till.
     */
    @Column(name = "valid_to")
    private LocalDate validTo;

    /**
     * Maximum Usage Count.
     */
    @Column(name = "maximum_usage")
    private Integer maximumUsage;

    /**
     * Current Usage Count.
     */
    @Builder.Default
    @Column(name = "used_count")
    private Integer usedCount = 0;

    /**
     * Can be applied on Custom Plan.
     */
    @Builder.Default
    @Column(name = "allow_custom_plan")
    private Boolean allowCustomPlan = true;

    /**
     * Stackable with another promotion.
     */
    @Builder.Default
    @Column(name = "stackable")
    private Boolean stackable = false;

    /**
     * Auto Apply.
     */
    @Builder.Default
    @Column(name = "auto_apply")
    private Boolean autoApply = false;

    /**
     * Promotion Status.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PromotionStatus status = PromotionStatus.ACTIVE;

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