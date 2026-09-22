package com.thinkerscave.finance.payroll.repository;

import com.thinkerscave.finance.payroll.entity.EmployeePayroll;
import com.thinkerscave.finance.payroll.enums.EmployeePayrollStatus;
import com.thinkerscave.finance.payroll.enums.PaymentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface EmployeePayrollRepository extends JpaRepository<EmployeePayroll, Long> {
    Optional<EmployeePayroll> findByStaffIdAndPayrollYearAndPayrollMonth(Long staffId, Integer year, Integer month);
    List<EmployeePayroll> findByPayrollRun_PayrollRunId(Long runId);
    List<EmployeePayroll> findByStaffIdOrderByPayrollYearDescPayrollMonthDesc(Long staffId);
    long countByPayrollYearAndPayrollMonth(Integer year, Integer month);
    long countByPayrollYearAndPayrollMonthAndStatus(Integer year, Integer month, EmployeePayrollStatus status);

    @Query("SELECT COALESCE(SUM(e.netAmount), 0) FROM EmployeePayroll e WHERE e.payrollYear = :year AND e.payrollMonth = :month")
    BigDecimal sumNetByYearMonth(@Param("year") Integer year, @Param("month") Integer month);

    @Query("""
            SELECT e FROM EmployeePayroll e
            WHERE e.payrollYear = :year AND e.payrollMonth = :month
              AND (:status IS NULL OR e.status = :status)
              AND (:paymentType IS NULL OR e.paymentType = :paymentType)
              AND (:staffId IS NULL OR e.staffId = :staffId)
            """)
    Page<EmployeePayroll> search(@Param("year") Integer year,
                                 @Param("month") Integer month,
                                 @Param("status") EmployeePayrollStatus status,
                                 @Param("paymentType") PaymentType paymentType,
                                 @Param("staffId") Long staffId,
                                 Pageable pageable);
}
