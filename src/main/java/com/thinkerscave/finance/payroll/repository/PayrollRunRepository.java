package com.thinkerscave.finance.payroll.repository;

import com.thinkerscave.finance.payroll.entity.PayrollRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PayrollRunRepository extends JpaRepository<PayrollRun, Long> {
    Optional<PayrollRun> findByPayrollYearAndPayrollMonth(Integer year, Integer month);
    Optional<PayrollRun> findByIdempotencyKey(String idempotencyKey);
}
