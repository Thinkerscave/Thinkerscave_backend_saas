package com.thinkerscave.finance.entity;

import com.thinkerscave.finance.enums.FeeFrequency;
import com.thinkerscave.finance.enums.FeeItemType;
import com.thinkerscave.finance.enums.FeeServiceKey;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "fee_structure_item",
        uniqueConstraints = @UniqueConstraint(name = "uk_fee_structure_item_head", columnNames = {"fee_structure_id", "fee_head_id"}),
        indexes = {
                @Index(name = "idx_fee_structure_item_structure", columnList = "fee_structure_id"),
                @Index(name = "idx_fee_structure_item_head", columnList = "fee_head_id")
        })
public class FeeStructureItem extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_structure_item_id")
    @EqualsAndHashCode.Include
    private Long feeStructureItemId;

    @Column(name = "fee_structure_id", nullable = false)
    private Long feeStructureId;

    @Column(name = "fee_head_id", nullable = false)
    private Long feeHeadId;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "frequency", nullable = false, length = 20)
    private FeeFrequency frequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private FeeItemType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_key", nullable = false, length = 20)
    private FeeServiceKey serviceKey = FeeServiceKey.NONE;
}
