package com.thinkerscave.student.repository;

import com.thinkerscave.common.student.domain.StudentAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentAchievementRepository extends JpaRepository<StudentAchievement, Long> {

    List<StudentAchievement> findByStudentIdAndOrganizationIdOrderByAchievementDateDesc(Long studentId, Long organizationId);

    List<StudentAchievement> findByOrganizationIdOrderByAchievementDateDesc(Long organizationId);

    long countByOrganizationId(Long organizationId);
}
