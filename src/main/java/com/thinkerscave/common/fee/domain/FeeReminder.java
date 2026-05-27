package com.thinkerscave.common.fee.domain;

import com.thinkerscave.common.common.entity.AuditableBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Individual reminder dispatch record for an overdue {@link FeeInvoice}. */
@Entity
@Table(name = "fee_reminder",
        indexes = {
                @Index(name = "idx_fee_remind_invoice", columnList = "fee_invoice_id"),
                @Index(name = "idx_fee_remind_sent",    columnList = "sent_at")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeeReminder extends AuditableBaseEntity {

    @Column(name = "fee_invoice_id", nullable = false)
    private Long feeInvoiceId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 16)
    private ReminderChannel channel;

    @Column(name = "recipient", nullable = false, length = 256)
    private String recipient;

    @Column(name = "subject", length = 256)
    private String subject;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "attempt_number")
    private Integer attemptNumber;
}
