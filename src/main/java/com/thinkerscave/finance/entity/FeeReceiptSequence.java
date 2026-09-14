package com.thinkerscave.finance.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "fee_receipt_sequence",
        uniqueConstraints = @UniqueConstraint(name = "uk_fee_receipt_sequence_key", columnNames = "sequence_key"))
public class FeeReceiptSequence extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_receipt_sequence_id")
    @EqualsAndHashCode.Include
    private Long feeReceiptSequenceId;

    @Column(name = "sequence_key", nullable = false, length = 40)
    private String sequenceKey;

    @Column(name = "last_value", nullable = false)
    private Long lastValue = 0L;
}
