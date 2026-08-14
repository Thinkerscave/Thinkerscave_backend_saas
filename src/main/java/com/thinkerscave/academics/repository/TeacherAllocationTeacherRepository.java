package com.thinkerscave.academics.repository;

import com.thinkerscave.academics.entity.TeacherAllocationTeacher;
import com.thinkerscave.academics.enums.TeacherAllocationTeacherRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherAllocationTeacherRepository extends JpaRepository<TeacherAllocationTeacher, Long> {

    List<TeacherAllocationTeacher> findByTeacherAllocation_TeacherAllocationIdOrderByEffectiveFromDesc(Long allocationId);

    Optional<TeacherAllocationTeacher> findByTeacherAllocation_TeacherAllocationIdAndRoleAndEffectiveToIsNull(
            Long allocationId, TeacherAllocationTeacherRole role);

    List<TeacherAllocationTeacher> findByStaff_StaffIdAndEffectiveToIsNull(Long staffId);
}
