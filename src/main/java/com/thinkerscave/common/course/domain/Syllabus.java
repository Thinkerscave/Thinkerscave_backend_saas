package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.course.enums.SyllabusStatus;
import com.thinkerscave.common.usrm.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Syllabus entity - curriculum for a subject with versioning support
 */
@Entity
@Table(name = "syllabus")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Syllabus extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "syllabus_id")
    private Long syllabusId;

    @Column(name = "syllabus_code", unique = true, length = 50)
    private String syllabusCode;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id")
    private AcademicYear academicYear;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "total_hours")
    private Integer totalHours;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "syllabus", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Chapter> chapters = new ArrayList<>();

    // Versioning fields
    @Column(name = "version", length = 20)
    private String version; // e.g., "1.0", "2.0"

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 50)
    private SyllabusStatus status = SyllabusStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_version_id")
    private Syllabus previousVersion; // Link to previous version

    // Approval workflow
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_date")
    private LocalDate approvedDate;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(name = "archived_date")
    private LocalDate archivedDate;

    @Column(columnDefinition = "TEXT")
    private String approval_remarks;
}
