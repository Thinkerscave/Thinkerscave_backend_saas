package com.thinkerscave.common.fee.repository;

import com.thinkerscave.common.fee.domain.FeeLedgerEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeeLedgerEntryRepository extends JpaRepository<FeeLedgerEntry, Long> {

    Page<FeeLedgerEntry> findByOrganizationIdAndStudentIdOrderByEntryDateAscIdAsc(
            Long organizationId, Long studentId, Pageable pageable);

    List<FeeLedgerEntry> findByFeeInvoiceId(Long feeInvoiceId);
}
