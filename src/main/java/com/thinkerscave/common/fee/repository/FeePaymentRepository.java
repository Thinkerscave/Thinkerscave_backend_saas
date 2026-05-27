package com.thinkerscave.common.fee.repository;

import com.thinkerscave.common.fee.domain.FeePayment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeePaymentRepository
        extends JpaRepository<FeePayment, Long>, JpaSpecificationExecutor<FeePayment> {

    Optional<FeePayment> findByOrganizationIdAndReceiptNumber(Long organizationId, String receiptNumber);

    Page<FeePayment> findByOrganizationIdAndStudentId(Long organizationId, Long studentId, Pageable pageable);

    List<FeePayment> findByOrganizationIdAndStudentIdOrderByPaymentDateDesc(Long organizationId, Long studentId);
}
