package com.thinkerscave.common.fee.repository;

import com.thinkerscave.common.fee.domain.FeeAdjustment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeAdjustmentRepository extends JpaRepository<FeeAdjustment, Long> {

    Page<FeeAdjustment> findByOrganizationIdAndStudentId(
            Long organizationId, Long studentId, Pageable pageable);

    List<FeeAdjustment> findByFeeInvoiceId(Long feeInvoiceId);
}
