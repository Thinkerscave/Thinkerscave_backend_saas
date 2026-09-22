package com.thinkerscave.finance.expense.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "expense_configuration")
public class ExpenseConfiguration extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "expense_configuration_id")
    @EqualsAndHashCode.Include
    private Long expenseConfigurationId;

    @Column(name = "approval_required", nullable = false)
    private Boolean approvalRequired = true;

    @Column(name = "number_prefix", nullable = false, length = 20)
    private String numberPrefix = "EXP";

    @Column(name = "number_year_format", nullable = false, length = 10)
    private String numberYearFormat = "YYYY";

    @Column(name = "number_pad_width", nullable = false)
    private Integer numberPadWidth = 6;

    @Column(name = "number_start", nullable = false)
    private Long numberStart = 1L;

    @Column(name = "number_sequence_per_year", nullable = false)
    private Boolean numberSequencePerYear = true;

    @Column(name = "default_payment_method_id")
    private Long defaultPaymentMethodId;

    @Column(name = "max_attachment_bytes", nullable = false)
    private Long maxAttachmentBytes = 10_485_760L;

    @Column(name = "allowed_attachment_content_types", nullable = false, length = 500)
    private String allowedAttachmentContentTypes = "application/pdf,image/jpeg,image/png";
}
