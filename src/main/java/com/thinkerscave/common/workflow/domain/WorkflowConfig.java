package com.thinkerscave.common.workflow.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import com.thinkerscave.common.enums.GenericStatus;
import jakarta.persistence.*;
import lombok.*;

/**
 * Per-organization workflow configuration — toggles whether modules use
 * approval flows, who approves, how many levels, etc.
 *
 * <p>Examples:
 * <ul>
 *   <li>{@code workflowKey=ADMISSION}, {@code approvalLevels=2}</li>
 *   <li>{@code workflowKey=FEE_REFUND},  {@code requireApproval=true}</li>
 *   <li>{@code workflowKey=TRANSFER_CERTIFICATE}, {@code requireApproval=true}</li>
 * </ul>
 *
 * <p>Stores approver chain as a comma-separated list of role codes (H2- and
 * Postgres-safe; can be parsed by the workflow engine).
 */
@Entity
@Table(name = "workflow_config",
        uniqueConstraints = @UniqueConstraint(name = "uk_workflow_org_key",
                columnNames = {"organization_id", "workflow_key"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkflowConfig extends OrganizationScopedEntity {

    @Column(name = "workflow_key", nullable = false, length = 64)
    private String workflowKey;

    @Column(name = "display_name", nullable = false, length = 128)
    private String displayName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "require_approval", nullable = false)
    private boolean requireApproval;

    @Column(name = "approval_levels")
    private Integer approvalLevels;

    @Column(name = "approver_role_codes", length = 500)
    private String approverRoleCodes;

    @Column(name = "auto_close_after_days")
    private Integer autoCloseAfterDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private GenericStatus status;
}
