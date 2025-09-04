package com.thinkerscave.common.student.repository;

import com.thinkerscave.common.student.domain.Guardian;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianRepository extends JpaRepository<Guardian,Long> {
}
