package com.thinkerscave.student.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.thinkerscave.student.entity.TransferRequest;

import java.util.List;

@Repository
public interface TransferRequestRepository
        extends JpaRepository<TransferRequest, Long>, JpaSpecificationExecutor<TransferRequest> {

    Page<TransferRequest> findByOrganizationId(Long organizationId, Pageable pageable);

    List<TransferRequest> findByOrganizationIdAndStudentId(Long organizationId, Long studentId);
}
