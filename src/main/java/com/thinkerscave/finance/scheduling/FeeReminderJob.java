package com.thinkerscave.finance.scheduling;

import com.thinkerscave.communication.dto.request.NotificationRequest;
import com.thinkerscave.communication.dto.response.NotificationResponse;
import com.thinkerscave.communication.service.NotificationService;
import com.thinkerscave.finance.entity.FeeReminderLog;
import com.thinkerscave.finance.entity.FeeReminderRule;
import com.thinkerscave.finance.entity.StudentBillingPeriod;
import com.thinkerscave.finance.enums.ReminderRuleKey;
import com.thinkerscave.finance.repository.FeeReminderLogRepository;
import com.thinkerscave.finance.repository.FeeReminderRuleRepository;
import com.thinkerscave.finance.repository.StudentBillingPeriodRepository;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Due-date reminders for outstanding billing periods across all completed tenants.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeeReminderJob {

    private final FinanceTenantJobRunner tenantJobRunner;
    private final FeeReminderRuleRepository reminderRuleRepository;
    private final FeeReminderLogRepository reminderLogRepository;
    private final StudentBillingPeriodRepository billingPeriodRepository;
    private final StudentRepository studentRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "${app.fees.reminder.cron:0 30 3 * * *}", zone = "UTC")
    public void sendReminders() {
        tenantJobRunner.forEachTenant("FeeReminderJob", tenant -> sendRemindersForCurrentTenant());
    }

    private void sendRemindersForCurrentTenant() {
        LocalDate today = LocalDate.now();
        List<FeeReminderRule> rules = reminderRuleRepository.findAll().stream()
                .filter(r -> Boolean.TRUE.equals(r.getEnabled()))
                .toList();
        if (rules.isEmpty()) {
            return;
        }

        List<StudentBillingPeriod> periods = billingPeriodRepository.findAll().stream()
                .filter(p -> p.getBalanceAmount() != null && p.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0)
                .filter(p -> p.getDueDate() != null)
                .toList();

        for (FeeReminderRule rule : rules) {
            for (StudentBillingPeriod period : periods) {
                if (!matches(rule, period.getDueDate(), today)) {
                    continue;
                }
                if (reminderLogRepository.existsByStudentBillingPeriodIdAndRuleKeyAndSentOn(
                        period.getStudentBillingPeriodId(), rule.getRuleKey(), today)) {
                    continue;
                }

                Long notificationId = null;
                try {
                    Student student = studentRepository.findById(period.getStudentId()).orElse(null);
                    if (student != null && student.getUser() != null && student.getUser().getId() != null) {
                        NotificationRequest request = new NotificationRequest();
                        request.setSubject("Fee reminder: " + period.getPeriodLabel());
                        request.setBody("Outstanding balance for " + period.getPeriodLabel()
                                + " is due on " + period.getDueDate() + ".");
                        request.setCategory("FINANCE");
                        request.setChannelsCsv(rule.getChannelsCsv() != null ? rule.getChannelsCsv() : "IN_APP");
                        request.setRecipientUserIds(List.of(student.getUser().getId()));
                        NotificationResponse sent = notificationService.send(request);
                        if (sent != null) {
                            notificationId = sent.getNotificationId();
                        }
                    }
                } catch (Exception ex) {
                    log.debug("Fee reminder notification skipped for period {}: {}",
                            period.getStudentBillingPeriodId(), ex.getMessage());
                }

                FeeReminderLog logRow = new FeeReminderLog();
                logRow.setStudentBillingPeriodId(period.getStudentBillingPeriodId());
                logRow.setRuleKey(rule.getRuleKey());
                logRow.setSentOn(today);
                logRow.setNotificationId(notificationId);
                reminderLogRepository.save(logRow);
            }
        }
    }

    private boolean matches(FeeReminderRule rule, LocalDate due, LocalDate today) {
        int offset = rule.getOffsetDays() == null ? 0 : rule.getOffsetDays();
        ReminderRuleKey key = rule.getRuleKey();
        if (key == null) {
            return false;
        }
        return switch (key) {
            case BEFORE_DUE -> today.equals(due.minusDays(offset));
            case ON_DUE -> today.equals(due);
            case AFTER_DUE, SECOND_REMINDER, FINAL_ESCALATION -> today.equals(due.plusDays(offset));
        };
    }
}
