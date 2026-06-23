package com.thinkerscave.staff.repository;

import com.thinkerscave.staff.entity.ResponsibilityAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ResponsibilityAssignmentRepository extends JpaRepository<ResponsibilityAssignment, Long> {

    List<ResponsibilityAssignment> findByStaff_StaffIdAndActiveTrueOrderByEffectiveFromDesc(Long staffId);

    boolean existsByStaff_StaffIdAndResponsibility_ResponsibilityIdAndActiveTrue(Long staffId, Long responsibilityId);
}
