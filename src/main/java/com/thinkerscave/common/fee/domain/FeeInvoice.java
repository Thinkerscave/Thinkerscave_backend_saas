package com.thinkerscave.common.fee.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A billable invoice generated from a {@link FeeContract} for a given
 * billing period. Line items are in {@link FeeInvoiceLine}.
 */
@Entity
@Table(name = "fee_invoice",
        uniqueConstraints = @UniqueConstraint(name = "uk_fee_invoice_number_org",
                columnNames = {"organization_id", "invoice_number"}),
        indexes = {
                @Index(name = "idx_fee_invoice_contract", columnList = "fee_contract_id"),
                @Index(name = "idx_fee_invoice_student",  columnList = "student_id"),
                @Index(name = "idx_fee_invoice_status",   columnList = "status"),
                @Index(name = "idx_fee_invoice_due",      columnList = "due_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeInvoice extends OrganizationScopedEntity {

    @Column(name = "invoice_number", nullable = false, length = 32)
    private String invoiceNumber;

    @Column(name = "fee_contract_id", nullable = false)
    private Long feeContractId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "enrollment_id", nullable = false)
    private Long enrollmentId;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "period_label", length = 32)
    private String periodLabel;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "subtotal", precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_total", precision = 12, scale = 2)
    private BigDecimal discountTotal;

    @Column(name = "tax_total", precision = 12, scale = 2)
    private BigDecimal taxTotal;

    @Column(name = "late_fee_total", precision = 12, scale = 2)
    private BigDecimal lateFeeTotal;

    @Column(name = "total_amount", precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", precision = 14, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "balance_amount", precision = 14, scale = 2)
    private BigDecimal balanceAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private InvoiceStatus status;

    @Column(name = "notes", length = 500)
    private String notes;
}
