package com.thinkerscave.common.student.repository;

import com.thinkerscave.common.student.domain.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Student domain with organization-scoped queries.
 * All queries filter by organization for data isolation within tenant.
 */
@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    /**
     * Find all students in an organization.
     * 
     * @param organizationId Organization ID
     * @return List of students in this organization
     */
    List<Student> findByOrganizationId(Long organizationId);

    /**
     * Find all active students in an organization.
     * 
     * @param organizationId Organization ID
     * @param isActive       Active status
     * @return List of active students
     */
    List<Student> findByOrganizationIdAndIsActive(Long organizationId, boolean isActive);

    /**
     * Find student by ID within organization.
     * Ensures cross-organization access is prevented.
     * 
     * @param studentId      Student ID
     * @param organizationId Organization ID
     * @return Student if found in this organization
     */
    Optional<Student> findByStudentIdAndOrganizationId(Long studentId, Long organizationId);

    /**
     * Find student by email within organization.
     * 
     * @param email          Student email
     * @param organizationId Organization ID
     * @return Student if found
     */
    Optional<Student> findByEmailAndOrganizationId(String email, Long organizationId);

    /**
     * Count students in an organization.
     * 
     * @param organizationId Organization ID
     * @return Number of students
     */
    @Query("SELECT COUNT(s) FROM Student s WHERE s.organizationId = :orgId")
    Long countByOrganizationId(@Param("orgId") Long organizationId);

    /**
     * Find students by class within organization.
     * 
     * @param classId        Class ID
     * @param organizationId Organization ID
     * @return Students in this class and organization
     */
    List<Student> findByClassEntityClassIdAndOrganizationId(Long classId, Long organizationId);
}
