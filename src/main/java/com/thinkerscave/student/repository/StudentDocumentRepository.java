package com.thinkerscave.student.repository;

import com.thinkerscave.student.entity.StudentDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentDocumentRepository extends JpaRepository<StudentDocument, Long> {

    List<StudentDocument> findByStudentStudentIdOrderByDocumentIdDesc(Long studentId);
}
