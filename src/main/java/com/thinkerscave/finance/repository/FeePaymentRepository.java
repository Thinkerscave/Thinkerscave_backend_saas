package com.thinkerscave.finance.repository;

import com.thinkerscave.finance.entity.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {

    Optional<FeePayment> findByIdempotencyKey(String idempotencyKey);

    List<FeePayment> findByStudentIdAndAcademicYearIdOrderByPaidOnDesc(Long studentId, Long academicYearId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM FeePayment p WHERE p.studentId = :studentId AND p.academicYearId = :yearId")
    BigDecimal sumPayments(@Param("studentId") Long studentId, @Param("yearId") Long yearId);

    @Query("""
            SELECT COALESCE(SUM(p.amount), 0) FROM FeePayment p
            WHERE p.academicYearId = :yearId
              AND (:studentIds IS NULL OR p.studentId IN :studentIds)
            """)
    BigDecimal sumPaymentsForYear(@Param("yearId") Long yearId, @Param("studentIds") List<Long> studentIds);

    @Query("""
            SELECT FUNCTION('YEAR', p.paidOn), FUNCTION('MONTH', p.paidOn), COALESCE(SUM(p.amount), 0)
            FROM FeePayment p
            WHERE p.academicYearId = :yearId
              AND (:studentIds IS NULL OR p.studentId IN :studentIds)
            GROUP BY FUNCTION('YEAR', p.paidOn), FUNCTION('MONTH', p.paidOn)
            ORDER BY FUNCTION('YEAR', p.paidOn), FUNCTION('MONTH', p.paidOn)
            """)
    List<Object[]> sumCollectedByMonth(@Param("yearId") Long yearId, @Param("studentIds") List<Long> studentIds);
}
