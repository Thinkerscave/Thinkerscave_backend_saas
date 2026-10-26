package com.thinkerscave.academics.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/** Audit trail for generation, validation, approval and publication transitions. */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "timetable_version_event",
        indexes = {
                @Index(name = "idx_timetable_version_event_version",
                        columnList = "timetable_version_id, created_on")
        }
)
public class TimetableVersionEvent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "timetable_version_event_id")
    @EqualsAndHashCode.Include
    private Long timetableVersionEventId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timetable_version_id", nullable = false)
    private TimetableVersion timetableVersion;

    @NotBlank
    @Column(name = "event_type", nullable = false, length = 40)
    private String eventType;

    @Column(name = "from_status", length = 40)
    private String fromStatus;

    @Column(name = "to_status", length = 40)
    private String toStatus;

    @Column(name = "note", length = 1000)
    private String note;

    @Column(name = "actor_user_id")
    private Long actorUserId;
}
