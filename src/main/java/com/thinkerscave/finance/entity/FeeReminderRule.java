package com.thinkerscave.finance.entity;

import com.thinkerscave.finance.enums.ReminderRuleKey;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(name = "fee_reminder_rule",
        uniqueConstraints = @UniqueConstraint(name = "uk_fee_reminder_rule_key", columnNames = "rule_key"))
public class FeeReminderRule extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_reminder_rule_id")
    @EqualsAndHashCode.Include
    private Long feeReminderRuleId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_key", nullable = false, length = 40)
    private ReminderRuleKey ruleKey;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    @Column(name = "offset_days", nullable = false)
    private Integer offsetDays = 0;

    @Column(name = "channels_csv", nullable = false, length = 100)
    private String channelsCsv = "IN_APP";
}
