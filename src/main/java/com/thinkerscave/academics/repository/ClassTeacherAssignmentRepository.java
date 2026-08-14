package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.ClassTeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassTeacherAssignmentRepository extends JpaRepository<ClassTeacherAssignment, Long> {

    Optional<ClassTeacherAssignment> findBySection_SectionIdAndEffectiveToIsNull(Long sectionId);

    List<ClassTeacherAssignment> findBySection_SectionIdOrderByEffectiveFromDesc(Long sectionId);

    List<ClassTeacherAssignment> findByStaff_StaffIdOrderByEffectiveFromDesc(Long staffId);
}
