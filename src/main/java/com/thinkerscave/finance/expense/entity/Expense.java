package com.thinkerscave.finance.expense.entity;

import com.thinkerscave.finance.expense.enums.ExpenseApprovalStatus;
import com.thinkerscave.finance.expense.enums.ExpensePaymentStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "expense", uniqueConstraints = @UniqueConstraint(name = "uk_expense_number", columnNames = "expense_number"))
public class Expense extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_id")
    @EqualsAndHashCode.Include
    private Long expenseId;

    @Column(name = "expense_number", nullable = false, length = 40)
    private String expenseNumber;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_head_id", nullable = false)
    private ExpenseHead expenseHead;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expense_category_id", nullable = false)
    private ExpenseCategory category;

    @Column(name = "head_name_snapshot", nullable = false, length = 150)
    private String headNameSnapshot;

    @Column(name = "category_code_snapshot", nullable = false, length = 40)
    private String categoryCodeSnapshot;

    @Column(name = "category_name_snapshot", nullable = false, length = 120)
    private String categoryNameSnapshot;

    @Column(name = "amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal amount;

    @Column(name = "vendor_name", length = 200)
    private String vendorName;

    @Column(name = "vendor_invoice_number", length = 100)
    private String vendorInvoiceNumber;

    @Column(name = "requester_staff_id", nullable = false)
    private Long requesterStaffId;

    @Column(name = "remarks", length = 2000)
    private String remarks;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30)
    private ExpenseApprovalStatus approvalStatus = ExpenseApprovalStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private ExpensePaymentStatus paymentStatus = ExpensePaymentStatus.UNPAID;

    @Column(name = "paid_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "remaining_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal remainingAmount;

    @Column(name = "submitted_on")
    private Instant submittedOn;

    @Column(name = "approved_on")
    private Instant approvedOn;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "rejected_on")
    private Instant rejectedOn;

    @Column(name = "rejected_by", length = 100)
    private String rejectedBy;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;
}
