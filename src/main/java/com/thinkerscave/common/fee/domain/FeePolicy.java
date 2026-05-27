package com.thinkerscave.common.fee.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Late fee / discount / reminder policy that can be attached to a
 * {@link FeeStructure}.
 */
@Entity
@Table(name = "fee_policy",
        uniqueConstraints = @UniqueConstraint(name = "uk_fee_policy_org_code",
                columnNames = {"organization_id", "code"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeePolicy extends OrganizationScopedEntity {

    @Column(name = "code", nullable = false, length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "grace_period_days")
    private Integer gracePeriodDays;

    @Column(name = "late_fee_amount", precision = 12, scale = 2)
    private BigDecimal lateFeeAmount;

    @Column(name = "late_fee_percent", precision = 5, scale = 2)
    private BigDecimal lateFeePercent;

    @Column(name = "compounding_days")
    private Integer compoundingDays;

    @Column(name = "max_late_fee", precision = 12, scale = 2)
    private BigDecimal maxLateFee;

    @Column(name = "early_bird_discount_percent", precision = 5, scale = 2)
    private BigDecimal earlyBirdDiscountPercent;

    @Column(name = "early_bird_cutoff_days")
    private Integer earlyBirdCutoffDays;

    @Column(name = "reminder_intervals_csv", length = 128)
    private String reminderIntervalsCsv;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
