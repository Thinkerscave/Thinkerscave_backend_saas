package com.thinkerscave.student.repository;

import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.enums.StudentStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

	boolean existsByAdmissionNumber(String admissionNumber);

	boolean existsByStudentCode(String studentCode);

	Optional<Student> findByAdmissionNumber(String admissionNumber);

	Optional<Student> findByStudentCode(String studentCode);

	Page<Student> findByStatus(StudentStatus status, Pageable pageable);

	long countByStatus(StudentStatus status);

	/** Used to resolve the logged-in Student's record for the Student dashboard. */
	Optional<Student> findByUser_Id(Long userId);

	@Query(value = """
			SELECT s FROM Student s
			WHERE (:status IS NULL OR s.status = :status)
			  AND (:keyword IS NULL OR
			       LOWER(CONCAT(COALESCE(s.firstName, ''), ' ', COALESCE(s.middleName, ''), ' ', COALESCE(s.lastName, ''))) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			       LOWER(CAST(s.studentId AS string)) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			       LOWER(COALESCE(s.studentCode, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			       LOWER(COALESCE(s.admissionNumber, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			       LOWER(COALESCE(s.rollNumber, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			       LOWER(CAST(COALESCE(s.mobileNumber, 0) AS string)) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			       LOWER(COALESCE(s.email, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			       EXISTS (SELECT sp.studentParentId FROM StudentParent sp JOIN sp.parent p
			               WHERE sp.student = s AND sp.active = true AND (
			                   LOWER(CONCAT(COALESCE(p.firstName, ''), ' ', COALESCE(p.middleName, ''), ' ', COALESCE(p.lastName, ''))) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			                   LOWER(COALESCE(p.mobileNumber, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))))
			  AND (:rollNumber IS NULL OR LOWER(COALESCE(s.rollNumber, '')) LIKE LOWER(CONCAT('%', CAST(:rollNumber AS string), '%')) OR EXISTS (
			       SELECT e.enrollmentId FROM StudentEnrollment e WHERE e.student = s AND e.active = true
			       AND LOWER(COALESCE(e.rollNumber, '')) LIKE LOWER(CONCAT('%', CAST(:rollNumber AS string), '%'))))
			  AND (:parentMobile IS NULL OR EXISTS (SELECT sp.studentParentId FROM StudentParent sp JOIN sp.parent p
			       WHERE sp.student = s AND sp.active = true
			       AND LOWER(COALESCE(p.mobileNumber, '')) LIKE LOWER(CONCAT('%', CAST(:parentMobile AS string), '%'))))
			  AND (:parentName IS NULL OR EXISTS (SELECT sp.studentParentId FROM StudentParent sp JOIN sp.parent p
			       WHERE sp.student = s AND sp.active = true
			       AND LOWER(CONCAT(COALESCE(p.firstName, ''), ' ', COALESCE(p.middleName, ''), ' ', COALESCE(p.lastName, ''))) LIKE LOWER(CONCAT('%', CAST(:parentName AS string), '%'))))
			  AND (:academicYearId IS NULL OR EXISTS (SELECT e.enrollmentId FROM StudentEnrollment e
			       WHERE e.student = s AND e.active = true AND e.academicYear.academicYearId = :academicYearId))
			  AND (:classId IS NULL OR EXISTS (SELECT e.enrollmentId FROM StudentEnrollment e
			       WHERE e.student = s AND e.active = true AND e.classEntity.classId = :classId))
			  AND (:sectionId IS NULL OR EXISTS (SELECT e.enrollmentId FROM StudentEnrollment e
			       WHERE e.student = s AND e.active = true AND e.section.sectionId = :sectionId))
			  AND (:assigned IS NULL OR (:assigned = true AND EXISTS (SELECT e.enrollmentId FROM StudentEnrollment e
			       WHERE e.student = s AND e.active = true)) OR (:assigned = false AND NOT EXISTS (
			       SELECT e.enrollmentId FROM StudentEnrollment e WHERE e.student = s AND e.active = true)))
			""",
			countQuery = """
			SELECT COUNT(s) FROM Student s
			WHERE (:status IS NULL OR s.status = :status)
			  AND (:keyword IS NULL OR
			       LOWER(CONCAT(COALESCE(s.firstName, ''), ' ', COALESCE(s.middleName, ''), ' ', COALESCE(s.lastName, ''))) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			       LOWER(CAST(s.studentId AS string)) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			       LOWER(COALESCE(s.studentCode, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			       LOWER(COALESCE(s.admissionNumber, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			       LOWER(COALESCE(s.rollNumber, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			       LOWER(CAST(COALESCE(s.mobileNumber, 0) AS string)) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			       LOWER(COALESCE(s.email, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			       EXISTS (SELECT sp.studentParentId FROM StudentParent sp JOIN sp.parent p
			               WHERE sp.student = s AND sp.active = true AND (
			                   LOWER(CONCAT(COALESCE(p.firstName, ''), ' ', COALESCE(p.middleName, ''), ' ', COALESCE(p.lastName, ''))) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			                   LOWER(COALESCE(p.mobileNumber, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')))))
			  AND (:rollNumber IS NULL OR LOWER(COALESCE(s.rollNumber, '')) LIKE LOWER(CONCAT('%', CAST(:rollNumber AS string), '%')) OR EXISTS (
			       SELECT e.enrollmentId FROM StudentEnrollment e WHERE e.student = s AND e.active = true
			       AND LOWER(COALESCE(e.rollNumber, '')) LIKE LOWER(CONCAT('%', CAST(:rollNumber AS string), '%'))))
			  AND (:parentMobile IS NULL OR EXISTS (SELECT sp.studentParentId FROM StudentParent sp JOIN sp.parent p
			       WHERE sp.student = s AND sp.active = true
			       AND LOWER(COALESCE(p.mobileNumber, '')) LIKE LOWER(CONCAT('%', CAST(:parentMobile AS string), '%'))))
			  AND (:parentName IS NULL OR EXISTS (SELECT sp.studentParentId FROM StudentParent sp JOIN sp.parent p
			       WHERE sp.student = s AND sp.active = true
			       AND LOWER(CONCAT(COALESCE(p.firstName, ''), ' ', COALESCE(p.middleName, ''), ' ', COALESCE(p.lastName, ''))) LIKE LOWER(CONCAT('%', CAST(:parentName AS string), '%'))))
			  AND (:academicYearId IS NULL OR EXISTS (SELECT e.enrollmentId FROM StudentEnrollment e
			       WHERE e.student = s AND e.active = true AND e.academicYear.academicYearId = :academicYearId))
			  AND (:classId IS NULL OR EXISTS (SELECT e.enrollmentId FROM StudentEnrollment e
			       WHERE e.student = s AND e.active = true AND e.classEntity.classId = :classId))
			  AND (:sectionId IS NULL OR EXISTS (SELECT e.enrollmentId FROM StudentEnrollment e
			       WHERE e.student = s AND e.active = true AND e.section.sectionId = :sectionId))
			  AND (:assigned IS NULL OR (:assigned = true AND EXISTS (SELECT e.enrollmentId FROM StudentEnrollment e
			       WHERE e.student = s AND e.active = true)) OR (:assigned = false AND NOT EXISTS (
			       SELECT e.enrollmentId FROM StudentEnrollment e WHERE e.student = s AND e.active = true)))
			""")
	Page<Student> searchDirectory(
			@Param("keyword") String keyword,
			@Param("rollNumber") String rollNumber,
			@Param("parentMobile") String parentMobile,
			@Param("parentName") String parentName,
			@Param("academicYearId") Long academicYearId,
			@Param("classId") Long classId,
			@Param("sectionId") Long sectionId,
			@Param("assigned") Boolean assigned,
			@Param("status") StudentStatus status,
			Pageable pageable);

	@Query("""
			SELECT s FROM Student s WHERE
			LOWER(s.firstName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			LOWER(s.lastName) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			LOWER(s.studentCode) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%')) OR
			LOWER(COALESCE(s.email, '')) LIKE LOWER(CONCAT('%', CAST(:keyword AS string), '%'))
			""")
	Page<Student> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
