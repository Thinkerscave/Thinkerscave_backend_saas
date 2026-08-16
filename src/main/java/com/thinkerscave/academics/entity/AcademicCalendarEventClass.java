package com.thinkerscave.academics.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(
        name = "academic_calendar_event_class",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ace_class", columnNames = {"event_id", "class_id"})
        },
        indexes = {
                @Index(name = "idx_ace_class_event", columnList = "event_id"),
                @Index(name = "idx_ace_class_class", columnList = "class_id")
        }
)
public class AcademicCalendarEventClass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private AcademicCalendarEvent event;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "class_id", nullable = false)
    private AcademicClass academicClass;
}
