package com.thinkerscave.common.fee.repository;

import com.thinkerscave.common.fee.domain.FeeReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeReminderRepository extends JpaRepository<FeeReminder, Long> {
    List<FeeReminder> findByFeeInvoiceId(Long feeInvoiceId);
    long countByFeeInvoiceId(Long feeInvoiceId);
}
