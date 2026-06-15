package com.thinkerscave.student.repository;

import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.enums.StudentStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

	boolean existsByAdmissionNumber(String admissionNumber);

	boolean existsByStudentCode(String studentCode);

	Optional<Student> findByAdmissionNumber(String admissionNumber);

	Optional<Student> findByStudentCode(String studentCode);

	Page<Student> findByStatus(StudentStatus status, Pageable pageable);
}
