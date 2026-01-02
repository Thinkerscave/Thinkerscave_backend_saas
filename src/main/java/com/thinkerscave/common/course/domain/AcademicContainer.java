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
 * 🏢 AcademicContainer Entity - The Structural DNA of Institutions
 * 
 * 🏛️ Business Purpose:
 * AcademicContainer is a polymorphic structural entity that models the physical
 * or logical groupings of students. It can represent a 'Class 10-A' in a
 * school,
 * a 'Computer Science Branch' in a college, or a 'January Batch' in a training
 * center. It solves the problem of "Rigid Schemas" by allowing institutions to
 * define their own unique hierarchy.
 * 
 * 👥 User Roles & Stakeholders:
 * - **School Principals / HODs**: Design the tree structure (e.g., Department
 * ->
 * Branch -> Section).
 * - **Teachers**: Assigned to specific containers (e.g., "Class Teacher of
 * 10-B").
 * - **Students**: Enrolled into these containers, which then dictates their
 * timetable and syllabus.
 * - **Infrastructure Manager**: Uses the 'capacity' field to manage classroom
 * allocation and prevent overcrowding.
 * 
 * 🔄 Academic Flow Position:
 * This is the **Structural Foundation**. Once a Course and Academic Year are
 * set, containers are created to hold the actual students and assignments.
 * Progress is often tracked at the container level for class-wide reporting.
 * 
 * 🏗️ Design Intent:
 * Implements a **Adjacency List Pattern** (parentContainer attribute). This
 * recursive structure allows for infinite nesting depth (e.g., School -> Wing
 * ->
 * Block -> Grade -> Section).
 */
@Entity
@Table(name = "academic_containers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class AcademicContainer extends Auditable {

    /**
     * Primary stable internal identifier for the hierarchical unit.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "container_id")
    private Long containerId;

    /**
     * Classification showing what this node represents (e.g., CLASS, SECTION,
     * BRANCH, YEAR, SEMESTER).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "container_type", nullable = false, length = 50)
    private ContainerType containerType;

    /**
     * Unique Business Identifier (e.g., SEC-10A-2024).
     * Used in ID cards, attendance sheets, and external reporting.
     */
    @Column(name = "container_code", unique = true, nullable = false, length = 50)
    private String containerCode;

    /**
     * Human-friendly label (e.g., "Section A", "Mechanical Engineering").
     */
    @Column(name = "container_name", nullable = false, length = 255)
    private String containerName;

    /**
     * Detailed notes about the container (e.g., "Located in West Wing, Lab 3").
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * The tenant organisation owning this part of the structure.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organisation organization;

    /**
     * The specific session this container is valid for.
     * Business Logic: Containers are usually recreated or cloned every year
     * to start with fresh student strengths.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    /**
     * The degree or certification program this container belongs to.
     * (e.g., The '2024 Batch' belongs to the 'B.Tech' Course).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    private Course course;

    /**
     * Link to the higher-level node in the tree.
     * Example: The parent of 'Section A' might be 'Grade 10'.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_container_id")
    private AcademicContainer parentContainer;

    /**
     * Immediate children of this node.
     * Cascade Rule: Deleting a branch removes its nested years/sections.
     */
    @OneToMany(mappedBy = "parentContainer", cascade = CascadeType.ALL)
    private List<AcademicContainer> childContainers = new ArrayList<>();

    /**
     * Depth indicator (Level 1 = Root, Level 2 = Sub-folder, etc.).
     * Used for building consistent breadcrumbs and UI tree views.
     */
    @Column(name = "level")
    private Integer level;

    /**
     * Maximum number of students allowed in this physical/logical unit.
     * Purpose: Alert management during the enrollment phase.
     */
    @Column(name = "capacity")
    private Integer capacity;

    /**
     * Number of students currently assigned.
     * Derived Field: Updated by the Admission service during enrollment.
     */
    @Column(name = "current_strength")
    private Integer currentStrength;

    /**
     * UI priority weight for sorting in lists.
     */
    @Column(name = "display_order")
    private Integer displayOrder;

    /**
     * Visibility flag for decommissioning old structures.
     */
    @Column(name = "is_active")
    private Boolean isActive = true;

    /**
     * Extensible data store for institutional-specific requirements.
     * Uses: Store JSON strings for attributes like "Morning/Evening Shift", "Room
     * Number".
     */
    @Column(columnDefinition = "TEXT")
    private String metadata;
}
