package com.thinkerscave.academics.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** A reusable daily timeline (ordered teaching periods and breaks) for one academic year. */
@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "schedule_pattern",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_schedule_pattern_year_name",
                        columnNames = {"academic_year_id", "name"})
        },
        indexes = {
                @Index(name = "idx_schedule_pattern_year_active", columnList = "academic_year_id, is_active")
        }
)
public class SchedulePattern extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_pattern_id")
    @EqualsAndHashCode.Include
    private Long schedulePatternId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Size(max = 500)
    @Column(name = "description", length = 500)
    private String description;

    @NotNull
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @OneToMany(mappedBy = "schedulePattern", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("periodNumber ASC")
    private List<TimetablePeriod> slots = new ArrayList<>();

    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }
}
