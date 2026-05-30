package com.thinkerscave.common.admin.domain;

import com.thinkerscave.common.common.entity.AuditableBaseEntity;
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

@Entity
@Table(name = "system_event", indexes = {
        @Index(name = "idx_system_event_org_time", columnList = "organization_id,occurred_at"),
        @Index(name = "idx_system_event_category", columnList = "category"),
        @Index(name = "idx_system_event_severity", columnList = "severity")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemEvent extends AuditableBaseEntity {

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "tenant_code", length = 64)
    private String tenantCode;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "component", nullable = false, length = 100)
    private String component;

    @Column(name = "event_code", nullable = false, length = 100)
    private String eventCode;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "severity", nullable = false, length = 20)
    private String severity;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "metric_name", length = 100)
    private String metricName;

    @Column(name = "metric_value")
    private Double metricValue;

    @Column(name = "metric_unit", length = 30)
    private String metricUnit;

    @Column(name = "resolved", nullable = false)
    private Boolean resolved = false;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}