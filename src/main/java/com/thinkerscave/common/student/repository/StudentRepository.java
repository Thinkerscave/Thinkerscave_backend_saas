package com.thinkerscave.common.student.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.thinkerscave.common.student.domain.Student;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

}
