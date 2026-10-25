package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.Subject;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long>, JpaSpecificationExecutor<Subject> {

    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndSubjectIdNot(String code, Long subjectId);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndSubjectIdNot(String name, Long subjectId);

    Optional<Subject> findFirstByCodeIgnoreCase(String code);

    List<Subject> findByActiveTrueOrderByNameAsc();

    long countByActiveTrue();
}
