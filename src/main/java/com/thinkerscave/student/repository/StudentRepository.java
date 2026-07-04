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

	@Query("""
			SELECT s FROM Student s WHERE
			LOWER(s.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
			LOWER(s.lastName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
			LOWER(s.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
			LOWER(s.admissionNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
			LOWER(COALESCE(s.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
			""")
	Page<Student> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
