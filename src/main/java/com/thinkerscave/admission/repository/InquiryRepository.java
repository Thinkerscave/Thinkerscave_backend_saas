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

    Optional<Inquiry> findByInquiryIdAndOrganizationIdAndDeletedFalse(Long id, Long orgId);

    Page<Inquiry> findByOrganizationIdAndDeletedFalseOrderByCreatedOnDesc(Long orgId, Pageable pageable);

    List<Inquiry> findByOrganizationIdAndStatusAndDeletedFalseOrderByCreatedOnDesc(Long orgId, InquiryStatus status);

    List<Inquiry> findByOrganizationIdAndDeletedFalseAndNextFollowUpDateLessThanEqualOrderByNextFollowUpDateAsc(
            Long orgId, LocalDate today);

    boolean existsByMobileNumberAndOrganizationIdAndDeletedFalse(String mobileNumber, Long orgId);

        boolean existsByInquiryIdAndOrganizationIdAndDeletedFalse(Long inquiryId, Long orgId);

    @Query("""
            SELECT i.status, COUNT(i)
            FROM Inquiry i
            WHERE i.organizationId = :orgId AND i.deleted = false
            GROUP BY i.status
            """)
    List<Object[]> countByStatusForOrg(@Param("orgId") Long orgId);

        @Query("""
            SELECT COALESCE(i.inquirySource, 'Unknown'), COUNT(i)
            FROM Inquiry i
            WHERE i.organizationId = :orgId AND i.deleted = false
            GROUP BY i.inquirySource
            """)
        List<Object[]> countBySourceForOrg(@Param("orgId") Long orgId);

        @Query("""
            SELECT i.assignedCounselorId, COUNT(i)
            FROM Inquiry i
            WHERE i.organizationId = :orgId AND i.deleted = false
            GROUP BY i.assignedCounselorId
            """)
        List<Object[]> countByCounselorForOrg(@Param("orgId") Long orgId);

    long countByOrganizationIdAndDeletedFalse(Long orgId);

    long countByOrganizationIdAndStatusAndDeletedFalse(Long orgId, InquiryStatus status);

    @Query("""
            SELECT i FROM Inquiry i
            WHERE i.organizationId = :orgId AND i.deleted = false AND (
                LOWER(i.name) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
                LOWER(i.mobileNumber) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
                LOWER(COALESCE(i.email, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
            )
            ORDER BY i.createdOn DESC
            """)
    Page<Inquiry> searchByOrganization(
            @Param("orgId") Long orgId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
