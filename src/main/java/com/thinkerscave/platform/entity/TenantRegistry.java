package com.thinkerscave.platform.entity;

import com.thinkerscave.platform.enums.ProvisionStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(
        name = "tenant_registry",
        indexes = {
                @Index(name = "idx_tenant_identifier", columnList = "tenant_identifier"),
                @Index(name = "idx_schema_name", columnList = "schema_name"),
                @Index(name = "idx_tenant_status", columnList = "provision_status")
        }
)
public class TenantRegistry extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Unique Tenant Identifier.
     * Example : TEN000001
     */
    @Column(name = "tenant_identifier", nullable = false, unique = true, length = 50)
    private String tenantIdentifier;

    /**
     * Organization mapped with this tenant.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false, unique = true)
    private Organization organization;

    /**
     * PostgreSQL Schema Name.
     * Example : org_1001
     */
    @Column(name = "schema_name", nullable = false, unique = true, length = 100)
    private String schemaName;

    /**
     * Database Version.
     */
    @Column(name = "database_version", length = 50)
    private String databaseVersion;

    /**
     * Migration Version.
     */
    @Column(name = "migration_version", length = 50)
    private String migrationVersion;

    /**
     * Provisioning Template Version.
     */
    @Column(name = "template_version", length = 50)
    private String templateVersion;

    /**
     * Provisioning Status.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "provision_status", nullable = false, length = 30)
    private ProvisionStatus provisionStatus = ProvisionStatus.PENDING;

    /**
     * Database Size (MB).
     */
    @Column(name = "database_size_mb")
    private Long databaseSizeMb;

    /**
     * Storage Used (MB).
     */
    @Column(name = "storage_used_mb")
    private Long storageUsedMb;

    @Column(name = "student_count")
    private Integer studentCount;

    @Column(name = "staff_count")
    private Integer staffCount;

    @Column(name = "branch_count")
    private Integer branchCount;

    @Column(name = "class_count")
    private Integer classCount;

    @Column(name = "section_count")
    private Integer sectionCount;

    @Column(name = "usage_refreshed_at")
    private LocalDateTime usageRefreshedAt;

    /**
     * Last Migration Time.
     */
    @Column(name = "last_migration_at")
    private LocalDateTime lastMigrationAt;

    /**
     * Last Backup Time.
     */
    @Column(name = "last_backup_at")
    private LocalDateTime lastBackupAt;

    /**
     * Last Health Check Time.
     */
    @Column(name = "last_health_check_at")
    private LocalDateTime lastHealthCheckAt;

    /**
     * Tenant Domain.
     */
    @Column(name = "tenant_domain", length = 255)
    private String tenantDomain;

    /**
     * Custom Domain.
     */
    @Column(name = "custom_domain", length = 255)
    private String customDomain;

    /**
     * Whether Maintenance Mode is Enabled.
     */
    @Builder.Default
    @Column(name = "maintenance_mode")
    private Boolean maintenanceMode = false;

    /**
     * Whether Tenant is Active.
     */
    @Builder.Default
    @Column(name = "active")
    private Boolean active = true;

    /**
     * Internal Remarks.
     */
    @Column(name = "remarks", length = 1000)
    private String remarks;

}