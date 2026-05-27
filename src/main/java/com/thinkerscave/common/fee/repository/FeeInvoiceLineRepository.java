package com.thinkerscave.common.fee.repository;

import com.thinkerscave.common.fee.domain.FeeInvoiceLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeInvoiceLineRepository extends JpaRepository<FeeInvoiceLine, Long> {
    List<FeeInvoiceLine> findByFeeInvoiceId(Long feeInvoiceId);
    void deleteByFeeInvoiceId(Long feeInvoiceId);
}
