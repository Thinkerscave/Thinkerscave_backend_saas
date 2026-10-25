package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.SectionTeacherAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SectionTeacherAssignmentRepository extends JpaRepository<SectionTeacherAssignment, Long> {

    Optional<SectionTeacherAssignment> findBySection_SectionIdAndEffectiveToIsNull(Long sectionId);

    Optional<SectionTeacherAssignment> findFirstBySection_SectionIdAndActiveTrueAndEffectiveToIsNullOrderByEffectiveFromDesc(
            Long sectionId);

    List<SectionTeacherAssignment> findBySection_SectionIdInAndActiveTrueAndEffectiveToIsNull(List<Long> sectionIds);

    List<SectionTeacherAssignment> findBySection_SectionIdOrderByEffectiveFromDesc(Long sectionId);

    List<SectionTeacherAssignment> findByStaff_StaffIdOrderByEffectiveFromDesc(Long staffId);
}
