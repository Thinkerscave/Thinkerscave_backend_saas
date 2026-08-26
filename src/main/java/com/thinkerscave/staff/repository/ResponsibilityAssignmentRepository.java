package com.thinkerscave.staff.repository;

import com.thinkerscave.staff.entity.ResponsibilityAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResponsibilityAssignmentRepository extends JpaRepository<ResponsibilityAssignment, Long> {

    List<ResponsibilityAssignment> findByStaff_StaffIdAndActiveTrueOrderByEffectiveFromDesc(Long staffId);

    boolean existsByStaff_StaffIdAndResponsibility_ResponsibilityIdAndActiveTrue(Long staffId, Long responsibilityId);

    Optional<ResponsibilityAssignment> findFirstByStaff_StaffIdAndResponsibility_ResponsibilityIdOrderByAssignmentIdDesc(
            Long staffId, Long responsibilityId);

    @Query("""
            SELECT a FROM ResponsibilityAssignment a
            JOIN FETCH a.staff s
            LEFT JOIN FETCH s.user
            WHERE a.responsibility.responsibilityId = :responsibilityId AND a.active = true
            ORDER BY s.firstName ASC, s.lastName ASC
            """)
    List<ResponsibilityAssignment> findActiveByResponsibilityId(@Param("responsibilityId") Long responsibilityId);
}
