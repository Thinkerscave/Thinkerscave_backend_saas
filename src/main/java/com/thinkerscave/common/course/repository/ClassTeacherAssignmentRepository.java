package com.thinkerscave.common.course.repository;

import com.thinkerscave.common.course.domain.AcademicYear;
import com.thinkerscave.common.course.domain.ClassTeacherAssignment;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.staff.domain.Staff;
import com.thinkerscave.common.student.domain.ClassEntity;
import com.thinkerscave.common.student.domain.Section;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassTeacherAssignmentRepository extends JpaRepository<ClassTeacherAssignment, Long> {

    @EntityGraph(attributePaths = { "organization", "academicYear", "classEntity", "section", "classTeacher" })
    List<ClassTeacherAssignment> findByOrganizationAndAcademicYearAndIsActiveTrueOrderByClassEntity_ClassNameAscSection_SectionNameAsc(
            Organisation organization,
            AcademicYear academicYear);

    @EntityGraph(attributePaths = { "organization", "academicYear", "classEntity", "section", "classTeacher" })
    Optional<ClassTeacherAssignment> findByAssignmentIdAndOrganization(Long assignmentId, Organisation organization);

    @Query("""
            select count(assignment) > 0
            from ClassTeacherAssignment assignment
            where assignment.organization = :organization
              and assignment.academicYear = :academicYear
              and assignment.classEntity = :classEntity
              and ((:section is null and assignment.section is null) or assignment.section = :section)
              and assignment.isActive = true
              and (:excludeId is null or assignment.assignmentId <> :excludeId)
            """)
    boolean existsActiveForClassSection(
            @Param("organization") Organisation organization,
            @Param("academicYear") AcademicYear academicYear,
            @Param("classEntity") ClassEntity classEntity,
            @Param("section") Section section,
            @Param("excludeId") Long excludeId);

    @Query("""
            select count(assignment) > 0
            from ClassTeacherAssignment assignment
            where assignment.organization = :organization
              and assignment.academicYear = :academicYear
              and assignment.classTeacher = :teacher
              and assignment.isActive = true
              and (:excludeId is null or assignment.assignmentId <> :excludeId)
            """)
    boolean existsActiveForTeacher(
            @Param("organization") Organisation organization,
            @Param("academicYear") AcademicYear academicYear,
            @Param("teacher") Staff teacher,
            @Param("excludeId") Long excludeId);
}