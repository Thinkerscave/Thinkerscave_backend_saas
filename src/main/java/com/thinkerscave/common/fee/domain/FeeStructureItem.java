package com.thinkerscave.common.fee.domain;

import com.thinkerscave.common.common.entity.AuditableBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * One line of a {@link FeeStructure} — couples a fee head + frequency +
 * amount + due-day rule.
 */
@Entity
@Table(name = "fee_structure_item",
        indexes = {
                @Index(name = "idx_fee_struct_item_struct", columnList = "fee_structure_id"),
                @Index(name = "idx_fee_struct_item_head",   columnList = "fee_head_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeStructureItem extends AuditableBaseEntity {

    @Column(name = "fee_structure_id", nullable = false)
    private Long feeStructureId;

    @Column(name = "fee_head_id", nullable = false)
    private Long feeHeadId;

    @Column(name = "fee_group_id")
    private Long feeGroupId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 16)
    private FeeFrequency frequency;

    /** For periodic fees: 1–28 day-of-month. For one-time: ignored. */
    @Column(name = "due_day_of_month")
    private Integer dueDayOfMonth;

    @Column(name = "optional", nullable = false)
    private boolean optional;

    @Column(name = "display_order")
    private Integer displayOrder;
}
