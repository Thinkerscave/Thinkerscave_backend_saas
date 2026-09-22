package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.response.TenantHealthResponse;
import com.thinkerscave.platform.dto.response.TenantHealthSummaryResponse;
import com.thinkerscave.platform.dto.response.TenantHealthPageResponse;
import com.thinkerscave.platform.entity.PlatformRelease;
import com.thinkerscave.platform.entity.TenantRegistry;
import com.thinkerscave.platform.enums.ReleaseStatus;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.repository.PlatformReleaseRepository;
import com.thinkerscave.platform.repository.TenantMigrationExecutionRepository;
import com.thinkerscave.platform.repository.TenantRegistryRepository;
import com.thinkerscave.platform.enums.OperationStatus;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class TenantHealthService {
    private final TenantRegistryRepository tenantRepository;
    private final OrganizationRepository organizationRepository;
    private final PlatformReleaseRepository releaseRepository;
    private final TenantMigrationExecutionRepository migrationRepository;
    private final TenantScopedFlyway tenantFlyway;
    private final JdbcTemplate jdbcTemplate;

    public List<TenantHealthResponse> checkAll() {
        PlatformRelease deployed = deployedRelease();
        return tenantRepository.findAll().stream().filter(t -> Boolean.TRUE.equals(t.getActive()))
                .map(tenant -> check(tenant, deployed)).toList();
    }

    public TenantHealthResponse check(Long tenantId) {
        return check(tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("TenantRegistry not found: " + tenantId)),
                deployedRelease());
    }

    public TenantHealthSummaryResponse summary() {
        return summarize(checkAll());
    }

    public TenantHealthPageResponse tenants(String search, Pageable pageable) {
        TenantHealthSummaryResponse summary = summarize(checkAll());
        PlatformRelease deployed = deployedRelease();
        Page<TenantHealthResponse> tenants = tenantRepository.searchTenants(null, search, pageable)
                .map(tenant -> check(tenant, deployed));
        return new TenantHealthPageResponse(summary, tenants);
    }

    private TenantHealthResponse check(TenantRegistry tenant, PlatformRelease deployed) {
        LocalDateTime now = LocalDateTime.now();
        String organizationName = organizationRepository.findById(tenant.getOrganization().getId())
                .map(organization -> organization.getOrganizationName())
                .orElse("Unknown organization");
        String status = "HEALTHY";
        List<String> issues = new ArrayList<>();
        String dbVersion = null;
        Long storage = tenant.getStorageUsedMb();
        OperationStatus migrationStatus = migrationRepository.findTopByTenant_IdOrderByCreatedOnDesc(tenant.getId())
                .map(execution -> execution.getStatus()).orElse(OperationStatus.PENDING);
        try {
            dbVersion = tenantFlyway.currentVersion(tenant);
            storage = jdbcTemplate.queryForObject("""
                    SELECT COALESCE(sum(pg_total_relation_size(
                        quote_ident(schemaname) || '.' || quote_ident(tablename))), 0) / 1048576
                    FROM pg_tables WHERE schemaname=?
                    """, Long.class, tenant.getSchemaName());
            if (Boolean.TRUE.equals(tenant.getMaintenanceMode())) {
                status = "MAINTENANCE";
                issues.add(tenant.getMaintenanceReason() == null ? "Tenant is in maintenance" : tenant.getMaintenanceReason());
            } else if (migrationStatus == OperationStatus.FAILED) {
                status = "WARNING";
                issues.add("Latest tenant migration failed");
            } else if (dbVersion == null) {
                status = "WARNING";
                issues.add("Tenant has no isolated Flyway history");
            } else if (deployed != null) {
                if (!deployed.getTargetDatabaseVersion().equals(dbVersion)) {
                    status = "WARNING";
                    issues.add("Database version differs from released target");
                }
                if (!deployed.getApplicationVersion().equals(tenant.getApplicationVersion())) {
                    status = "WARNING";
                    issues.add("Application version differs from released target");
                }
                if (!deployed.getTargetCatalogVersion().equals(String.valueOf(tenant.getCatalogVersion()))) {
                    status = "WARNING";
                    issues.add("Catalog version differs from released target");
                }
            }
        } catch (Exception ex) {
            status = "CRITICAL";
            issues.add(ex.getMessage() == null ? "Tenant schema is unreachable" : ex.getMessage());
        }
        int score = switch (status) {
            case "HEALTHY" -> 100;
            case "WARNING" -> 70;
            case "MAINTENANCE" -> 50;
            default -> 0;
        };
        return new TenantHealthResponse(tenant.getId(), organizationName,
                tenant.getTenantIdentifier(), tenant.getSchemaName(), dbVersion,
                tenant.getCatalogVersion() == null ? null : String.valueOf(tenant.getCatalogVersion()),
                migrationStatus, storage, tenant.getProvisionStatus(), status, score,
                Boolean.TRUE.equals(tenant.getMaintenanceMode()), now, issues);
    }

    private PlatformRelease deployedRelease() {
        return releaseRepository.findTopByStatusOrderByReleasedAtDesc(ReleaseStatus.RELEASED).orElse(null);
    }

    private TenantHealthSummaryResponse summarize(List<TenantHealthResponse> rows) {
        return new TenantHealthSummaryResponse(rows.size(),
                rows.stream().filter(r -> "HEALTHY".equals(r.healthStatus())).count(),
                rows.stream().filter(r -> "WARNING".equals(r.healthStatus())).count(),
                rows.stream().filter(r -> "MAINTENANCE".equals(r.healthStatus())).count(),
                rows.stream().filter(r -> "CRITICAL".equals(r.healthStatus())).count(),
                LocalDateTime.now());
    }
}
