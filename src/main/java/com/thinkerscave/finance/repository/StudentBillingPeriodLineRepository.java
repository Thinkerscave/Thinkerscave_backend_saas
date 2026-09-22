package com.thinkerscave.finance.repository;

import com.thinkerscave.finance.entity.StudentBillingPeriodLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentBillingPeriodLineRepository extends JpaRepository<StudentBillingPeriodLine, Long> {

    List<StudentBillingPeriodLine> findByStudentBillingPeriodIdOrderByStudentBillingPeriodLineIdAsc(
            Long studentBillingPeriodId);

    boolean existsByFeeHeadId(Long feeHeadId);
}
