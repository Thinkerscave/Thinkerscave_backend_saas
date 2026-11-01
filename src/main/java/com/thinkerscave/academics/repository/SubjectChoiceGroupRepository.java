package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.SubjectChoiceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SubjectChoiceGroupRepository extends JpaRepository<SubjectChoiceGroup, Long>,
        JpaSpecificationExecutor<SubjectChoiceGroup> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndSubjectChoiceGroupIdNot(String name, Long subjectChoiceGroupId);

    Optional<SubjectChoiceGroup> findById(Long subjectChoiceGroupId);
}
