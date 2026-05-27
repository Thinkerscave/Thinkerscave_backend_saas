package com.thinkerscave.common.exam.domain;

import com.thinkerscave.common.common.entity.AuditableBaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

/** Per-subject sitting — date, time, room, invigilator. */
@Entity
@Table(name = "exam_schedule",
        indexes = {
                @Index(name = "idx_exam_sched_exam",    columnList = "exam_id"),
                @Index(name = "idx_exam_sched_subject", columnList = "subject_id"),
                @Index(name = "idx_exam_sched_date",    columnList = "exam_date")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamSchedule extends AuditableBaseEntity {

    @Column(name = "exam_id", nullable = false)
    private Long examId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "room", length = 64)
    private String room;

    @Column(name = "invigilator_staff_id")
    private Long invigilatorStaffId;

    @Column(name = "notes", length = 500)
    private String notes;
}
