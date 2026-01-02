package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.usrm.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * SyllabusAccess - Track read-only access to syllabus
 * For students and parents viewing syllabus content
 */
@Entity
@Table(name = "syllabus_access")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class SyllabusAccess extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_id")
    private Long accessId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Student or Parent

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "syllabus_id", nullable = false)
    private Syllabus syllabus;

    @Column(name = "first_accessed_date")
    private LocalDateTime firstAccessedDate;

    @Column(name = "last_accessed_date")
    private LocalDateTime lastAccessedDate;

    @Column(name = "view_count")
    private Integer viewCount = 0;

    @Column(name = "total_time_spent_minutes")
    private Integer totalTimeSpentMinutes; // Cumulative time

    @Column(name = "access_type", length = 50)
    private String accessType; // STUDENT, PARENT, TEACHER

    @Column(name = "device_type", length = 50)
    private String deviceType; // WEB, MOBILE, TABLET
}
