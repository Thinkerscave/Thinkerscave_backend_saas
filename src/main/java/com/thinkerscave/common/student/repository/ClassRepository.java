package com.thinkerscave.common.student.repository;


import com.thinkerscave.common.student.domain.ClassEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassRepository extends JpaRepository<ClassEntity,Long> {
}
