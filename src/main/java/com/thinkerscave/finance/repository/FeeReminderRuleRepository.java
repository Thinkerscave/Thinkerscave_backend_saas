package com.thinkerscave.finance.repository;

import com.thinkerscave.finance.entity.FeeReminderRule;
import com.thinkerscave.finance.enums.ReminderRuleKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeeReminderRuleRepository extends JpaRepository<FeeReminderRule, Long> {

    Optional<FeeReminderRule> findByRuleKey(ReminderRuleKey ruleKey);

    List<FeeReminderRule> findAllByOrderByFeeReminderRuleIdAsc();

    List<FeeReminderRule> findByEnabledTrue();
}
