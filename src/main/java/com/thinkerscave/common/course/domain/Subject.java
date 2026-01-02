package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.course.enums.SubjectCategory;
import com.thinkerscave.common.orgm.domain.Organisation;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Subject entity - individual subjects/courses
 * Examples: Mathematics, Physics, Data Structures, etc.
 */
@Entity
@Table(name = "subjects")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class Subject extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "subject_id")
    private Long subjectId;

    @Column(name = "subject_code", unique = true, nullable = false, length = 50)
    private String subjectCode;

    @Column(name = "subject_name", nullable = false, length = 255)
    private String subjectName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "credits")
    private Integer credits;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 50)
    private SubjectCategory category;

    @Column(name = "theory_hours")
    private Integer theoryHours;

    @Column(name = "practical_hours")
    private Integer practicalHours;

    @Column(name = "lab_hours")
    private Integer labHours;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organisation organization;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @OneToMany(mappedBy = "subject", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Syllabus> syllabi = new ArrayList<>();

    @Column(name = "max_marks")
    private Integer maxMarks;

    @Column(name = "passing_marks")
    private Integer passingMarks;
}
