package com.thinkerscave.finance.payroll.repository;

import com.thinkerscave.finance.payroll.entity.EmployeeSalary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EmployeeSalaryRepository extends JpaRepository<EmployeeSalary, Long> {
    /** Prefer this when duplicates may exist (legacy migration / re-assign). */
    Optional<EmployeeSalary> findFirstByStaffIdAndActiveTrueOrderByEffectiveFromDescEmployeeSalaryIdDesc(Long staffId);

    @Deprecated
    Optional<EmployeeSalary> findByStaffIdAndActiveTrue(Long staffId);

    List<EmployeeSalary> findByStaffIdAndActiveTrueOrderByEffectiveFromDesc(Long staffId);
    List<EmployeeSalary> findByStaffIdOrderByEffectiveFromDesc(Long staffId);
    List<EmployeeSalary> findByActiveTrue();

    @Query("""
            SELECT e FROM EmployeeSalary e
            WHERE e.staffId = :staffId
              AND e.effectiveFrom <= :periodEnd
              AND (e.effectiveTo IS NULL OR e.effectiveTo >= :periodStart)
            ORDER BY e.effectiveFrom DESC
            """)
    List<EmployeeSalary> findCoveringPeriod(@Param("staffId") Long staffId,
                                            @Param("periodStart") LocalDate periodStart,
                                            @Param("periodEnd") LocalDate periodEnd);
}
