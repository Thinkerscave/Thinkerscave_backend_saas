package com.thinkerscave.finance.repository;

import com.thinkerscave.finance.entity.StudentBillingPeriod;
import com.thinkerscave.finance.enums.BillingPeriodStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StudentBillingPeriodRepository extends JpaRepository<StudentBillingPeriod, Long> {

    List<StudentBillingPeriod> findByStudentIdAndAcademicYearIdOrderByDueDateAscPeriodStartAscStudentBillingPeriodIdAsc(
            Long studentId, Long academicYearId);

    Optional<StudentBillingPeriod> findByStudentIdAndAcademicYearIdAndPeriodKey(
            Long studentId, Long academicYearId, String periodKey);

    boolean existsByFeeStructureId(Long feeStructureId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT p FROM StudentBillingPeriod p
            WHERE p.studentId = :studentId AND p.academicYearId = :yearId AND p.balanceAmount > 0
            ORDER BY p.dueDate ASC, p.periodStart ASC, p.studentBillingPeriodId ASC
            """)
    List<StudentBillingPeriod> findOpenPeriodsForUpdate(@Param("studentId") Long studentId,
                                                        @Param("yearId") Long yearId);

    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM StudentBillingPeriod p WHERE p.studentId = :studentId AND p.academicYearId = :yearId")
    BigDecimal sumTotal(@Param("studentId") Long studentId, @Param("yearId") Long yearId);

    @Query("SELECT COALESCE(SUM(p.paidAmount), 0) FROM StudentBillingPeriod p WHERE p.studentId = :studentId AND p.academicYearId = :yearId")
    BigDecimal sumPaid(@Param("studentId") Long studentId, @Param("yearId") Long yearId);

    @Query("SELECT COALESCE(SUM(p.balanceAmount), 0) FROM StudentBillingPeriod p WHERE p.studentId = :studentId AND p.academicYearId = :yearId")
    BigDecimal sumBalance(@Param("studentId") Long studentId, @Param("yearId") Long yearId);

    @Query("""
            SELECT COALESCE(SUM(p.totalAmount), 0) FROM StudentBillingPeriod p
            WHERE p.academicYearId = :yearId
              AND (:studentIds IS NULL OR p.studentId IN :studentIds)
            """)
    BigDecimal sumTotalForYear(@Param("yearId") Long yearId, @Param("studentIds") List<Long> studentIds);

    @Query("""
            SELECT COALESCE(SUM(p.balanceAmount), 0) FROM StudentBillingPeriod p
            WHERE p.academicYearId = :yearId AND p.balanceAmount > 0
              AND (:studentIds IS NULL OR p.studentId IN :studentIds)
            """)
    BigDecimal sumOutstandingForYear(@Param("yearId") Long yearId, @Param("studentIds") List<Long> studentIds);

    @Query("""
            SELECT COALESCE(SUM(p.balanceAmount), 0) FROM StudentBillingPeriod p
            WHERE p.academicYearId = :yearId AND p.balanceAmount > 0 AND p.status = :status
              AND (:studentIds IS NULL OR p.studentId IN :studentIds)
            """)
    BigDecimal sumByStatus(@Param("yearId") Long yearId,
                           @Param("status") BillingPeriodStatus status,
                           @Param("studentIds") List<Long> studentIds);

    @Query("""
            SELECT FUNCTION('YEAR', p.dueDate), FUNCTION('MONTH', p.dueDate), COALESCE(SUM(p.totalAmount), 0)
            FROM StudentBillingPeriod p
            WHERE p.academicYearId = :yearId
              AND (:studentIds IS NULL OR p.studentId IN :studentIds)
            GROUP BY FUNCTION('YEAR', p.dueDate), FUNCTION('MONTH', p.dueDate)
            ORDER BY FUNCTION('YEAR', p.dueDate), FUNCTION('MONTH', p.dueDate)
            """)
    List<Object[]> sumDueByMonth(@Param("yearId") Long yearId, @Param("studentIds") List<Long> studentIds);
}
