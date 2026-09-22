package com.thinkerscave.finance.payroll.entity;

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
@Table(name = "payroll_payment", indexes = {
        @Index(name = "idx_payroll_payment_ep", columnList = "employee_payroll_id")
})
public class PayrollPayment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "payroll_payment_id")
    @EqualsAndHashCode.Include
    private Long payrollPaymentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_payroll_id", nullable = false)
    private EmployeePayroll employeePayroll;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "paid_on", nullable = false)
    private LocalDate paidOn;

    @Column(name = "payment_method_id")
    private Long paymentMethodId;

    @Column(name = "payment_method_name", length = 80)
    private String paymentMethodName;

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @Column(name = "idempotency_key", nullable = false, length = 120, unique = true)
    private String idempotencyKey;
}
