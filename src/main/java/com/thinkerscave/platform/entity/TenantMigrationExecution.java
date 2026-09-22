package com.thinkerscave.platform.entity;

import com.thinkerscave.platform.enums.OperationStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenant_migration_executions")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class TenantMigrationExecution extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "release_id")
    private PlatformRelease release;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_registry_id", nullable = false)
    private TenantRegistry tenant;

    @Column(name = "from_version", length = 50)
    private String fromVersion;

    @Column(name = "target_version", nullable = false, length = 50)
    private String targetVersion;

    @Column(name = "observed_version", length = 50)
    private String observedVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private OperationStatus status = OperationStatus.PENDING;

    @Builder.Default
    private Integer attempt = 1;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", length = 4000)
    private String errorMessage;
}
