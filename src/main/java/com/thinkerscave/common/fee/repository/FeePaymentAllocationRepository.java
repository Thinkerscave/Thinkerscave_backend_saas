package com.thinkerscave.common.fee.repository;

import com.thinkerscave.common.fee.domain.FeePaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeePaymentAllocationRepository extends JpaRepository<FeePaymentAllocation, Long> {
    List<FeePaymentAllocation> findByFeePaymentId(Long feePaymentId);
    List<FeePaymentAllocation> findByFeeInvoiceId(Long feeInvoiceId);
}
