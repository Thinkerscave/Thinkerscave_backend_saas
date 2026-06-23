package com.thinkerscave.staff.repository;

import com.thinkerscave.staff.entity.Payroll;
import com.thinkerscave.staff.enums.PayrollStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    boolean existsByStaff_StaffIdAndPayrollYearAndPayrollMonth(Long staffId, Integer year, Integer month);

    Optional<Payroll> findByStaff_StaffIdAndPayrollYearAndPayrollMonth(Long staffId, Integer year, Integer month);

    List<Payroll> findByPayrollYearAndPayrollMonth(Integer year, Integer month);

    @Query("""
            SELECT p FROM Payroll p
            WHERE (:year IS NULL OR p.payrollYear = :year)
              AND (:month IS NULL OR p.payrollMonth = :month)
              AND (:status IS NULL OR p.status = :status)
              AND (:staffId IS NULL OR p.staff.staffId = :staffId)
            ORDER BY p.payrollYear DESC, p.payrollMonth DESC
            """)
    Page<Payroll> searchPayroll(
            @Param("year") Integer year,
            @Param("month") Integer month,
            @Param("status") PayrollStatus status,
            @Param("staffId") Long staffId,
            Pageable pageable
    );

    List<Payroll> findByStaff_StaffIdOrderByPayrollYearDescPayrollMonthDesc(Long staffId);

    @Query("SELECT COUNT(p) FROM Payroll p WHERE p.payrollYear = :year AND p.payrollMonth = :month AND p.status = :status")
    long countByYearAndMonthAndStatus(@Param("year") Integer year, @Param("month") Integer month, @Param("status") PayrollStatus status);

    @Query("SELECT COUNT(p) FROM Payroll p WHERE p.payrollYear = :year AND p.payrollMonth = :month")
    long countByYearAndMonth(@Param("year") Integer year, @Param("month") Integer month);
}
