package com.thinkerscave.finance.entity;

import com.thinkerscave.finance.enums.FeeReceiptStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "fee_receipt",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fee_receipt_number", columnNames = "receipt_number"),
                @UniqueConstraint(name = "uk_fee_receipt_payment", columnNames = "fee_payment_id")
        })
public class FeeReceipt extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_receipt_id")
    @EqualsAndHashCode.Include
    private Long feeReceiptId;

    @Column(name = "fee_payment_id", nullable = false)
    private Long feePaymentId;

    @Column(name = "receipt_number", nullable = false, length = 60)
    private String receiptNumber;

    @Column(name = "issued_on", nullable = false)
    private LocalDateTime issuedOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FeeReceiptStatus status = FeeReceiptStatus.ISSUED;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", nullable = false, length = 200)
    private String studentName;

    @Column(name = "admission_number", nullable = false, length = 50)
    private String admissionNumber;

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(name = "section_name", length = 100)
    private String sectionName;

    @Column(name = "academic_year_id", nullable = false)
    private Long academicYearId;

    @Column(name = "academic_year_name", nullable = false, length = 50)
    private String academicYearName;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "payment_method_name", nullable = false, length = 80)
    private String paymentMethodName;

    @Column(name = "reference_number", length = 80)
    private String referenceNumber;

    @Column(name = "remarks", length = 500)
    private String remarks;

    @Column(name = "school_name", nullable = false, length = 200)
    private String schoolName;

    @Column(name = "school_logo_url", length = 500)
    private String schoolLogoUrl;

    @Column(name = "school_address", length = 500)
    private String schoolAddress;

    @Column(name = "school_contact", length = 200)
    private String schoolContact;

    @Column(name = "currency_code", length = 10)
    private String currencyCode;
}
