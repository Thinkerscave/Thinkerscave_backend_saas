package com.thinkerscave.student.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thinkerscave.student.entity.StudentMedical;

@Repository
public interface StudentMedicalRepository extends JpaRepository<StudentMedical, Long> {

	Optional<StudentMedical> findByStudentStudentId(Long studentId);
}
