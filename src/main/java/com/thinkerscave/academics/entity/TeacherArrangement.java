package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.ArrangementStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "teacher_arrangement",
        indexes = {
                @Index(name = "idx_arrangement_status", columnList = "status")
        }
)
public class TeacherArrangement extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "arrangement_id")
    @EqualsAndHashCode.Include
    private Long arrangementId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private TimetableSlot timetableSlot;

    @Column(name = "absent_teacher_id", nullable = false)
    private Long absentTeacherId;

    @Column(name = "substitute_teacher_id", nullable = false)
    private Long substituteTeacherId;

    @Column(name = "arrangement_date", nullable = false)
    private LocalDate arrangementDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ArrangementStatus status = ArrangementStatus.PENDING;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "active")
    private Boolean active = true;
}