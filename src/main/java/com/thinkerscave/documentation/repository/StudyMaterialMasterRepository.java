package com.thinkerscave.documentation.repository;

import com.thinkerscave.documentation.entity.StudyMaterialMaster;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface StudyMaterialMasterRepository extends JpaRepository<StudyMaterialMaster, Long> {
    @Query("""
            SELECT m FROM StudyMaterialMaster m
            WHERE m.organizationId = :orgId
              AND (:status IS NULL OR m.status = :status)
              AND (:keyword IS NULL OR LOWER(m.masterName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))
            """)
    Page<StudyMaterialMaster> search(@Param("orgId") Long orgId, @Param("status") String status,
                                     @Param("keyword") String keyword, Pageable pageable);

    List<StudyMaterialMaster> findByOrganizationIdAndStatus(Long organizationId, String status);
}
