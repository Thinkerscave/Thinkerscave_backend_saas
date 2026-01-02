package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * SubjectContainerMapping - Maps subjects to academic containers
 * Supports flexible mapping for different institution types:
 * - School: Subject → Class → Section
 * - College: Subject → Branch → Semester
 * - University: Subject → Department → Year
 */
@Entity
@Table(name = "subject_container_mapping")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SubjectContainerMapping extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private Long mappingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = false)
    private AcademicContainer container;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "semester")
    private Integer semester; // For semester-based systems

    @Column(name = "year")
    private Integer year; // For year-based systems

    @Column(name = "is_mandatory")
    private Boolean isMandatory = true; // Core vs Elective

    @Column(name = "credits")
    private Integer credits;

    @Column(name = "hours_per_week")
    private Integer hoursPerWeek;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "is_active")
    private Boolean isActive = true;
}
