package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.course.enums.ContainerType;
import com.thinkerscave.common.orgm.domain.Organisation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * AcademicContainer - Flexible container for different institution structures
 * 
 * Examples:
 * - School: Class 10-A, Class 12-B (type=CLASS)
 * - College: CS Branch, Mechanical Branch (type=BRANCH)
 * - University: Computer Science Department (type=DEPARTMENT)
 * - Training Center: Module 1, Batch Jan-2024 (type=MODULE/BATCH)
 * 
 * Supports hierarchical structures:
 * - Department → Year → Section
 * - Branch → Semester → Section
 * - Class → Section
 */
@Entity
@Table(name = "academic_containers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AcademicContainer extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "container_id")
    private Long containerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "container_type", nullable = false, length = 50)
    private ContainerType containerType;

    @Column(name = "container_code", unique = true, nullable = false, length = 50)
    private String containerCode;

    @Column(name = "container_name", nullable = false, length = 255)
    private String containerName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organisation organisation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course; // Link to course/program

    // Hierarchical support
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_container_id")
    private AcademicContainer parentContainer;

    @OneToMany(mappedBy = "parentContainer", cascade = CascadeType.ALL)
    private List<AcademicContainer> childContainers = new ArrayList<>();

    @Column(name = "level")
    private Integer level; // 1 = top level, 2 = nested, etc.

    @Column(name = "capacity")
    private Integer capacity; // Max students/participants

    @Column(name = "current_strength")
    private Integer currentStrength; // Current enrolled count

    @Column(name = "display_order")
    private Integer displayOrder; // For UI sorting

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String metadata; // JSON for flexible attributes
}
