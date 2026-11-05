package com.thinkerscave.student.repository;

import com.thinkerscave.student.entity.StudentImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentImportJobRepository extends JpaRepository<StudentImportJob, String> {
}
