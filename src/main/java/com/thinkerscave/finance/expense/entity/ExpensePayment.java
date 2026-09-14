package com.thinkerscave.finance.expense.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "expense_payment",
        uniqueConstraints = @UniqueConstraint(name = "uk_expense_payment_idem", columnNames = "idempotency_key"))
public class ExpensePayment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_payment_id")
    @EqualsAndHashCode.Include
    private Long expensePaymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_id", nullable = false)
    private Expense expense;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_on", nullable = false)
    private LocalDate paidOn;

    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @Column(name = "payment_method_name", length = 100)
    private String paymentMethodName;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "remarks", length = 1000)
    private String remarks;

    @Column(name = "idempotency_key", nullable = false, length = 120)
    private String idempotencyKey;
}
