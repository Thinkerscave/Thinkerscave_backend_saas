package com.thinkerscave.platform.dto.response;

import com.thinkerscave.platform.enums.ProvisionStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class TenantRegistryResponse {

    private Long id;
    private String tenantIdentifier;
    private Long organizationId;
    private String organizationName;
    private String schemaName;
    private String databaseVersion;
    private String migrationVersion;
    private String templateVersion;
    private ProvisionStatus provisionStatus;
    private Long databaseSizeMb;
    private Long storageUsedMb;
    private Integer studentCount;
    private Integer staffCount;
    private Integer branchCount;
    private Integer classCount;
    private Integer sectionCount;
    private LocalDateTime usageRefreshedAt;
    private LocalDateTime lastMigrationAt;
    private LocalDateTime lastBackupAt;
    private LocalDateTime lastHealthCheckAt;
    private String tenantDomain;
    private String customDomain;
    private Boolean maintenanceMode;
    private Boolean active;
    private String remarks;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime updatedOn;
    private String updatedBy;
}
