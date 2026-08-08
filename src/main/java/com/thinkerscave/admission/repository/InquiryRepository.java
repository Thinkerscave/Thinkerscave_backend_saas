package com.thinkerscave.admission.repository;

import com.thinkerscave.admission.entity.Inquiry;
import com.thinkerscave.admission.enums.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface InquiryRepository extends JpaRepository<Inquiry, Long>, JpaSpecificationExecutor<Inquiry> {

    // Schema-per-tenant: No organizationId filtering needed
    // Tenant isolation handled by schema context

    Optional<Inquiry> findByInquiryIdAndDeletedFalse(Long id);

    Page<Inquiry> findByDeletedFalseOrderByCreatedOnDesc(Pageable pageable);

    List<Inquiry> findByStatusAndDeletedFalseOrderByCreatedOnDesc(InquiryStatus status);

    List<Inquiry> findByDeletedFalseAndNextFollowUpDateLessThanEqualOrderByNextFollowUpDateAsc(LocalDate today);

    boolean existsByMobileNumberAndDeletedFalse(String mobileNumber);

    boolean existsByInquiryIdAndDeletedFalse(Long inquiryId);

    @Query("""
            SELECT i.status, COUNT(i)
            FROM Inquiry i
            WHERE i.deleted = false
            GROUP BY i.status
            """)
    List<Object[]> countByStatus();

    @Query("""
            SELECT COALESCE(i.inquirySource, 'Unknown'), COUNT(i)
            FROM Inquiry i
            WHERE i.deleted = false
            GROUP BY i.inquirySource
            """)
    List<Object[]> countBySource();

    @Query("""
            SELECT i.assignedCounselorId, COUNT(i)
            FROM Inquiry i
            WHERE i.deleted = false
            GROUP BY i.assignedCounselorId
            """)
    List<Object[]> countByCounselor();

    long countByDeletedFalse();

    long countByStatusAndDeletedFalse(InquiryStatus status);

    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.deleted = false AND (
                LOWER(i.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
                LOWER(i.mobileNumber) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
                LOWER(COALESCE(i.email, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
            )
            ORDER BY i.createdOn DESC
            """)
    Page<Inquiry> search(@Param("keyword") String keyword, Pageable pageable);
}
