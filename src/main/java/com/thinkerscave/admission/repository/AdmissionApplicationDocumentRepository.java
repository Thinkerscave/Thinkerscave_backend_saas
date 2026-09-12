package com.thinkerscave.admission.repository;

import com.thinkerscave.admission.entity.AdmissionApplicationDocument;
import com.thinkerscave.admission.enums.DocumentCheckStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AdmissionApplicationDocumentRepository extends JpaRepository<AdmissionApplicationDocument, Long> {

    List<AdmissionApplicationDocument> findByApplicationApplicationIdOrderByCreatedOnDesc(Long applicationId);

    long countByApplicationApplicationId(Long applicationId);

    List<AdmissionApplicationDocument> findByApplicationApplicationIdIn(Collection<Long> applicationIds);

    @Query("""
            SELECT d.status, COUNT(d)
            FROM AdmissionApplicationDocument d
            WHERE d.application.applicationId IN :applicationIds
            GROUP BY d.status
            """)
    List<Object[]> countByStatusForApplications(@Param("applicationIds") Collection<Long> applicationIds);

    long countByApplicationApplicationIdAndStatus(Long applicationId, DocumentCheckStatus status);
}
