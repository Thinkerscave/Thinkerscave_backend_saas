package com.thinkerscave.student.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.thinkerscave.student.entity.TransferRequest;

import java.util.List;

@Repository
public interface TransferRequestRepository
        extends JpaRepository<TransferRequest, Long>, JpaSpecificationExecutor<TransferRequest> {

    // Tenant isolation via schema - no organizationId filter needed
    List<TransferRequest> findByStudentId(Long studentId);
}
