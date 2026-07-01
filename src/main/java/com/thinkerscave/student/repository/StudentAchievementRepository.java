package com.thinkerscave.student.repository;

import com.thinkerscave.student.entity.StudentAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentAchievementRepository extends JpaRepository<StudentAchievement, Long> {

    @Query("""
            SELECT a FROM StudentAchievement a
            WHERE a.student.studentId = :studentId
              AND a.student.user.organizationId = :organizationId
            ORDER BY a.achievementDate DESC
            """)
    List<StudentAchievement> findByStudentIdAndOrganizationIdOrderByAchievementDateDesc(
            @Param("studentId") Long studentId,
            @Param("organizationId") Long organizationId);

    @Query("""
            SELECT a FROM StudentAchievement a
            WHERE a.student.user.organizationId = :organizationId
            ORDER BY a.achievementDate DESC
            """)
    List<StudentAchievement> findByOrganizationIdOrderByAchievementDateDesc(@Param("organizationId") Long organizationId);

    @Query("""
            SELECT COUNT(a) FROM StudentAchievement a
            WHERE a.student.user.organizationId = :organizationId
            """)
    long countByOrganizationId(@Param("organizationId") Long organizationId);
}
