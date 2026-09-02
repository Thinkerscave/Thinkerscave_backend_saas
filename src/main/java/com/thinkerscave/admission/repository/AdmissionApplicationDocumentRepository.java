package com.thinkerscave.admission.repository;

import com.thinkerscave.admission.entity.AdmissionApplicationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionApplicationDocumentRepository extends JpaRepository<AdmissionApplicationDocument, Long> {

    List<AdmissionApplicationDocument> findByApplicationApplicationIdOrderByCreatedOnDesc(Long applicationId);

    long countByApplicationApplicationId(Long applicationId);
}
