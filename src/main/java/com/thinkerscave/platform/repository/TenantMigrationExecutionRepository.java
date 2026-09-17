package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.TenantMigrationExecution;
import com.thinkerscave.platform.enums.OperationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TenantMigrationExecutionRepository extends JpaRepository<TenantMigrationExecution, Long> {
    Page<TenantMigrationExecution> findByTenant_IdOrderByCreatedOnDesc(Long tenantId, Pageable pageable);
    List<TenantMigrationExecution> findByRelease_IdOrderByIdAsc(Long releaseId);
    boolean existsByTenant_IdAndStatus(Long tenantId, OperationStatus status);
    Optional<TenantMigrationExecution> findTopByTenant_IdAndStatusOrderByCreatedOnDesc(
            Long tenantId, OperationStatus status);
    Optional<TenantMigrationExecution> findTopByTenant_IdOrderByCreatedOnDesc(Long tenantId);
}
