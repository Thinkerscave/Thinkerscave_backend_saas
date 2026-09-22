package com.thinkerscave.finance.entity;

import com.thinkerscave.finance.enums.FeeHeadCategory;
import com.thinkerscave.finance.enums.FeeMasterStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "fee_head", indexes = {
        @Index(name = "idx_fee_head_status", columnList = "status"),
        @Index(name = "idx_fee_head_category", columnList = "category")
})
public class FeeHead extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_head_id")
    @EqualsAndHashCode.Include
    private Long feeHeadId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "name_normalized", nullable = false, length = 100, unique = true)
    private String nameNormalized;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private FeeHeadCategory category;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeeMasterStatus status = FeeMasterStatus.ACTIVE;
}
