package com.thinkerscave.common.fee.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A payment received from / on behalf of a student. Can be allocated across
 * one or more invoices via {@link FeePaymentAllocation}.
 */
@Entity
@Table(name = "fee_payment",
        uniqueConstraints = @UniqueConstraint(name = "uk_fee_payment_receipt",
                columnNames = {"organization_id", "receipt_number"}),
        indexes = {
                @Index(name = "idx_fee_payment_student",  columnList = "student_id"),
                @Index(name = "idx_fee_payment_status",   columnList = "status"),
                @Index(name = "idx_fee_payment_date",     columnList = "payment_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeePayment extends OrganizationScopedEntity {

    @Column(name = "receipt_number", nullable = false, length = 32)
    private String receiptNumber;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "allocated_amount", precision = 14, scale = 2)
    private BigDecimal allocatedAmount;

    @Column(name = "unallocated_amount", precision = 14, scale = 2)
    private BigDecimal unallocatedAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 24)
    private PaymentMethod paymentMethod;

    @Column(name = "reference_number", length = 64)
    private String referenceNumber;

    @Column(name = "gateway_transaction_id", length = 128)
    private String gatewayTransactionId;

    @Column(name = "received_by_user_id")
    private Long receivedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private PaymentStatus status;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
