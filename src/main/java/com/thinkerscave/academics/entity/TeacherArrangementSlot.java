package com.thinkerscave.academics.entity;

import com.thinkerscave.shared.entity.Auditable;
import com.thinkerscave.staff.entity.Staff;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "academics_teacher_arrangement_slot",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_academics_teacher_arrangement_slot_entry",
                columnNames = {"teacher_arrangement_id", "timetable_entry_id"}
        ),
        indexes = {
                @Index(name = "idx_academics_teacher_arrangement_slot_entry", columnList = "timetable_entry_id"),
                @Index(name = "idx_academics_teacher_arrangement_slot_substitute", columnList = "substitute_staff_id")
        }
)
public class TeacherArrangementSlot extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "teacher_arrangement_slot_id")
    @EqualsAndHashCode.Include
    private Long teacherArrangementSlotId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "teacher_arrangement_id", nullable = false)
    private TeacherArrangement arrangement;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "timetable_entry_id", nullable = false)
    private TimetableEntry timetableEntry;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "substitute_staff_id", nullable = false)
    private Staff substituteTeacher;
}