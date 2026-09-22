package com.thinkerscave.finance.repository;

import com.thinkerscave.finance.entity.FeePaymentAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface FeePaymentAllocationRepository extends JpaRepository<FeePaymentAllocation, Long> {

    List<FeePaymentAllocation> findByFeePaymentId(Long feePaymentId);

    @Query("""
            SELECT COALESCE(SUM(a.allocatedAmount), 0) FROM FeePaymentAllocation a, FeePayment p
            WHERE a.feePaymentId = p.feePaymentId
              AND p.studentId = :studentId AND p.academicYearId = :yearId
            """)
    BigDecimal sumAllocations(@Param("studentId") Long studentId, @Param("yearId") Long yearId);

    @Query("""
            SELECT COALESCE(SUM(a.allocatedAmount), 0) FROM FeePaymentAllocation a
            WHERE a.feePaymentId = :paymentId
            """)
    BigDecimal sumForPayment(@Param("paymentId") Long paymentId);
}
