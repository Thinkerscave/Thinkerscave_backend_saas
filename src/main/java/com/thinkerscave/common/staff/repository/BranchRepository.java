package com.thinkerscave.common.staff.repository;

import com.thinkerscave.common.staff.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch,Long> {
}
