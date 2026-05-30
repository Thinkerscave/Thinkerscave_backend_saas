package com.thinkerscave.common.audit.domain;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Lightweight activity feed for dashboard widgets.
 *
 * <p>Records business events like "Student Created", "Fee Paid",
 * "Attendance Marked", etc. Designed for fast reads on dashboard
 * activity lists.
 */
@Entity
@Table(name = "activity_log", indexes = {
    @Index(name = "idx_activity_log_org_time", columnList = "organization_id, performed_at DESC"),
    @Index(name = "idx_activity_log_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_activity_log_performer", columnList = "performed_by"),
    @Index(name = "idx_activity_log_type_time", columnList = "entity_type, performed_at DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLog extends OrganizationScopedEntity {

    @Column(name = "entity_type", nullable = false, length = 64)
    private String entityType;

    @Column(name = "entity_id")
    private Long entityId;

    @Column(name = "action", nullable = false, length = 128)
    private String action;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "performed_by", nullable = false, length = 100)
    private String performedBy;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
}
