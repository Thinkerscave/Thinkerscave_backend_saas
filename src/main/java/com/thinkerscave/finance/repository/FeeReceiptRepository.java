package com.thinkerscave.finance.repository;

import com.thinkerscave.finance.entity.FeeReceipt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FeeReceiptRepository extends JpaRepository<FeeReceipt, Long> {

    Optional<FeeReceipt> findByFeePaymentId(Long feePaymentId);

    List<FeeReceipt> findByStudentIdAndAcademicYearIdOrderByIssuedOnDesc(Long studentId, Long academicYearId);

    @Query("""
            SELECT r FROM FeeReceipt r
            WHERE (:yearId IS NULL OR r.academicYearId = :yearId)
              AND (:studentIds IS NULL OR r.studentId IN :studentIds)
              AND (:q IS NULL OR LOWER(r.receiptNumber) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(r.studentName) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(r.admissionNumber) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
            """)
    Page<FeeReceipt> search(@Param("q") String q,
                            @Param("yearId") Long yearId,
                            @Param("studentIds") List<Long> studentIds,
                            Pageable pageable);

    @Query("""
            SELECT r FROM FeeReceipt r
            WHERE (:yearId IS NULL OR r.academicYearId = :yearId)
              AND (:studentIds IS NULL OR r.studentId IN :studentIds)
              AND (:q IS NULL OR LOWER(r.receiptNumber) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(r.studentName) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%'))
                   OR LOWER(r.admissionNumber) LIKE LOWER(CONCAT('%', CAST(:q AS string), '%')))
              AND (:paymentMethod IS NULL OR LOWER(r.paymentMethodName) = LOWER(CAST(:paymentMethod AS string)))
              AND (:className IS NULL OR LOWER(r.className) = LOWER(CAST(:className AS string)))
              AND (:sectionName IS NULL OR LOWER(r.sectionName) = LOWER(CAST(:sectionName AS string)))
              AND r.issuedOn >= :fromOn
              AND r.issuedOn <= :toOn
            """)
    Page<FeeReceipt> searchFiltered(@Param("q") String q,
                                    @Param("yearId") Long yearId,
                                    @Param("studentIds") List<Long> studentIds,
                                    @Param("paymentMethod") String paymentMethod,
                                    @Param("className") String className,
                                    @Param("sectionName") String sectionName,
                                    @Param("fromOn") LocalDateTime fromOn,
                                    @Param("toOn") LocalDateTime toOn,
                                    Pageable pageable);
}
