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
        name = "academic_calendar_event_group",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ace_group", columnNames = {"event_id", "academic_group_id"})
        },
        indexes = {
                @Index(name = "idx_ace_group_event", columnList = "event_id"),
                @Index(name = "idx_ace_group_group", columnList = "academic_group_id")
        }
)
public class AcademicCalendarEventGroup {

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
    @JoinColumn(name = "academic_group_id", nullable = false)
    private AcademicGroup academicGroup;
}
