package com.thinkerscave.platform.entity;

import com.thinkerscave.platform.enums.ProvisionJobStatus;
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
        name = "provisioning_jobs",
        indexes = {
                @Index(name = "idx_provision_job_code", columnList = "job_code"),
                @Index(name = "idx_provision_job_status", columnList = "status"),
                @Index(name = "idx_provision_job_org", columnList = "organization_id")
        }
)
public class ProvisioningJob extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Unique Provisioning Job Code.
     * Example : JOB000001
     */
    @Column(name = "job_code", nullable = false, unique = true, length = 50)
    private String jobCode;

    /**
     * Organization being provisioned.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /**
     * Tenant Registry.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_registry_id")
    private TenantRegistry tenantRegistry;

    /**
     * Provisioning Template Used.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private ProvisioningTemplate provisioningTemplate;

    /**
     * Job Status.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ProvisionJobStatus status = ProvisionJobStatus.PENDING;

    /**
     * Current Execution Step.
     */
    @Column(name = "current_step", length = 150)
    private String currentStep;

    /**
     * Progress Percentage.
     */
    @Builder.Default
    @Column(name = "progress_percentage")
    private Integer progressPercentage = 0;

    /**
     * Started At.
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * Completed At.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * Duration in Seconds.
     */
    @Column(name = "duration_seconds")
    private Long durationSeconds;

    /**
     * Retry Count.
     */
    @Builder.Default
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    /**
     * Error Message.
     */
    @Column(name = "error_message", length = 4000)
    private String errorMessage;

    /**
     * Error Stack Trace.
     */
    @Lob
    @Column(name = "error_stack_trace")
    private String errorStackTrace;

    /**
     * Provisioned By.
     */
    @Column(name = "provisioned_by", length = 100)
    private String provisionedBy;

    /**
     * Active Flag.
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