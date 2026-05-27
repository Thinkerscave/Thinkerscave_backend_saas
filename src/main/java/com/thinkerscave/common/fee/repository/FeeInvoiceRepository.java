package com.thinkerscave.common.fee.repository;

import com.thinkerscave.common.fee.domain.FeeInvoice;
import com.thinkerscave.common.fee.domain.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FeeInvoiceRepository
        extends JpaRepository<FeeInvoice, Long>, JpaSpecificationExecutor<FeeInvoice> {

    Optional<FeeInvoice> findByOrganizationIdAndInvoiceNumber(Long organizationId, String invoiceNumber);

    Page<FeeInvoice> findByOrganizationIdAndStudentId(Long organizationId, Long studentId, Pageable pageable);

    Page<FeeInvoice> findByOrganizationIdAndStatus(Long organizationId, InvoiceStatus status, Pageable pageable);

    List<FeeInvoice> findByOrganizationIdAndStatusAndDueDateBefore(
            Long organizationId, InvoiceStatus status, LocalDate cutoff);

    long countByOrganizationIdAndStatus(Long organizationId, InvoiceStatus status);
}
