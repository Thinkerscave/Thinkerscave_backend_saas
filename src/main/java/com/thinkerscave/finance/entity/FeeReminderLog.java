package com.thinkerscave.finance.entity;

import com.thinkerscave.finance.enums.ReminderRuleKey;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "fee_reminder_log",
        uniqueConstraints = @UniqueConstraint(name = "uk_fee_reminder_log",
                columnNames = {"student_billing_period_id", "rule_key", "sent_on"}))
public class FeeReminderLog extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_reminder_log_id")
    @EqualsAndHashCode.Include
    private Long feeReminderLogId;

    @Column(name = "student_billing_period_id", nullable = false)
    private Long studentBillingPeriodId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_key", nullable = false, length = 40)
    private ReminderRuleKey ruleKey;

    @Column(name = "sent_on", nullable = false)
    private LocalDate sentOn;

    @Column(name = "notification_id")
    private Long notificationId;
}
