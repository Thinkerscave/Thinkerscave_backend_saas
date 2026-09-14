package com.thinkerscave.finance.repository;

import com.thinkerscave.finance.entity.FeeReminderLog;
import com.thinkerscave.finance.enums.ReminderRuleKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface FeeReminderLogRepository extends JpaRepository<FeeReminderLog, Long> {

    Optional<FeeReminderLog> findByStudentBillingPeriodIdAndRuleKeyAndSentOn(
            Long studentBillingPeriodId, ReminderRuleKey ruleKey, LocalDate sentOn);

    boolean existsByStudentBillingPeriodIdAndRuleKeyAndSentOn(
            Long studentBillingPeriodId, ReminderRuleKey ruleKey, LocalDate sentOn);
}
