package com.thinkerscave.academics.entity;

import com.thinkerscave.academics.enums.DayOfWeek;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "timetable_slot",
        indexes = {
                @Index(name = "idx_slot_day", columnList = "day_of_week")
        }
)
public class TimetableSlot extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    @EqualsAndHashCode.Include
    private Long slotId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private AcademicClass academicClass;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private AcademicSection academicSection;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_assignment_id", nullable = false)
    private SubjectAssignment subjectAssignment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "period_template_id", nullable = false)
    private PeriodTemplate periodTemplate;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "active")
    private Boolean active = true;
}