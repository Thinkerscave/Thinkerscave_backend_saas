package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.ClassScheduleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClassScheduleAssignmentRepository extends JpaRepository<ClassScheduleAssignment, Long> {

    Optional<ClassScheduleAssignment> findByAcademicClass_ClassIdAndAcademicSection_SectionId(
            Long classId, Long sectionId);
}
