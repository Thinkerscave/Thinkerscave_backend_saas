package com.thinkerscave.audit.repository;

import com.thinkerscave.audit.entity.SecurityAuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface SecurityAuditLogRepository
        extends JpaRepository<SecurityAuditLog, Long>, JpaSpecificationExecutor<SecurityAuditLog> {

    Page<SecurityAuditLog> findByUsername(String username, Pageable pageable);
}
