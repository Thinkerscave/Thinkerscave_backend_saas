package com.thinkerscave.finance.payroll.repository;

import com.thinkerscave.finance.payroll.entity.PayrollPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PayrollPaymentRepository extends JpaRepository<PayrollPayment, Long> {
    Optional<PayrollPayment> findByIdempotencyKey(String idempotencyKey);
    List<PayrollPayment> findByEmployeePayroll_EmployeePayrollIdOrderByPaidOnAsc(Long employeePayrollId);
    boolean existsByEmployeePayroll_EmployeePayrollId(Long employeePayrollId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM PayrollPayment p WHERE p.employeePayroll.employeePayrollId = :id")
    BigDecimal sumByEmployeePayrollId(@Param("id") Long employeePayrollId);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM PayrollPayment p
            WHERE p.employeePayroll.payrollYear = :year AND p.employeePayroll.payrollMonth = :month
            """)
    BigDecimal sumByYearMonth(@Param("year") Integer year, @Param("month") Integer month);
}
