package com.thinkerscave.common.staff.repository;

import com.thinkerscave.common.staff.domain.StaffDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StaffDocumentRepository extends JpaRepository<StaffDocument, Long> {

    List<StaffDocument> findByOrganizationIdOrderByCreatedDateDesc(Long organizationId);

    List<StaffDocument> findByOrganizationIdAndCategoryOrderByCreatedDateDesc(Long organizationId, String category);

    List<StaffDocument> findByStaffIdAndOrganizationIdOrderByCreatedDateDesc(Long staffId, Long organizationId);

    Optional<StaffDocument> findByDocumentIdAndOrganizationId(Long documentId, Long organizationId);

    long countByOrganizationId(Long organizationId);

    long countByOrganizationIdAndStatus(Long organizationId, String status);
}
