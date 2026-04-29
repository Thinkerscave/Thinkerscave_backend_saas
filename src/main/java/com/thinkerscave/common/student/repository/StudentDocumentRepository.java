package com.thinkerscave.common.student.repository;

import com.thinkerscave.common.student.domain.StudentDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StudentDocumentRepository extends JpaRepository<StudentDocument, Long> {

    List<StudentDocument> findByStudentStudentIdAndOrganizationId(Long studentId, Long organizationId);

    Optional<StudentDocument> findByDocumentIdAndOrganizationId(Long documentId, Long organizationId);

}
