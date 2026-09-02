package com.thinkerscave.retention.entity;

import com.thinkerscave.retention.RetentionTrigger;
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
@Table(
        name = "retention_purge_log",
        indexes = {
                @Index(name = "idx_retention_purge_task_ran", columnList = "task_key, ran_at")
        }
)
public class RetentionPurgeLog extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "task_key", nullable = false, length = 64)
    private String taskKey;

    @Column(name = "retention_days", nullable = false)
    private int retentionDays;

    @Column(name = "cutoff_at", nullable = false)
    private LocalDateTime cutoffAt;

    @Column(name = "deleted_count", nullable = false)
    private int deletedCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false, length = 32)
    private RetentionTrigger triggerType;

    @Column(name = "organization_id")
    private Long organizationId;

    @Column(name = "actor_username", length = 100)
    private String actorUsername;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(name = "ran_at", nullable = false)
    private LocalDateTime ranAt;
}
