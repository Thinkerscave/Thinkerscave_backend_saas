package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {

    boolean existsBySubjectCode(String subjectCode);

    boolean existsBySubjectCodeAndSubjectIdNot(String code, Long id);

    List<Subject> findByActiveOrderBySubjectNameAsc(Boolean active);

    List<Subject> findBySubjectNameContainingIgnoreCaseOrSubjectCodeContainingIgnoreCase(
            String name, String code);
}
