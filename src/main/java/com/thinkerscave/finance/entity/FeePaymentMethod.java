package com.thinkerscave.finance.entity;

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
@Table(name = "fee_payment_method",
        uniqueConstraints = @UniqueConstraint(name = "uk_fee_payment_method_name", columnNames = "name_normalized"))
public class FeePaymentMethod extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_payment_method_id")
    @EqualsAndHashCode.Include
    private Long feePaymentMethodId;

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @Column(name = "name_normalized", nullable = false, length = 80)
    private String nameNormalized;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeeMasterStatus status = FeeMasterStatus.ACTIVE;

    @Column(name = "requires_reference", nullable = false)
    private Boolean requiresReference = false;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;
}
