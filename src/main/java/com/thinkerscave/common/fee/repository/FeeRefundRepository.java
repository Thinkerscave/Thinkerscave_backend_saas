package com.thinkerscave.common.fee.repository;

import com.thinkerscave.common.fee.domain.FeeRefund;
import com.thinkerscave.common.fee.domain.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FeeRefundRepository
        extends JpaRepository<FeeRefund, Long>, JpaSpecificationExecutor<FeeRefund> {

    Optional<FeeRefund> findByOrganizationIdAndRefundNumber(Long organizationId, String refundNumber);

    Page<FeeRefund> findByOrganizationIdAndStatus(Long organizationId, RefundStatus status, Pageable pageable);
}
