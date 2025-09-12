package com.thinkerscave.common.staff.repository;

import com.thinkerscave.common.staff.domain.Branch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch,Long> {

    Optional<Branch> findByBranchCode(String branchCode);

    List<Branch> findAllByIsActiveTrue();


}
