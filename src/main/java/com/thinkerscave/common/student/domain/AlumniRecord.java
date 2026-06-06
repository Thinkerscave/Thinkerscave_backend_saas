package com.thinkerscave.common.student.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@Table(name = "alumni_record", indexes = {
    @Index(name = "idx_alumni_org", columnList = "organization_id"),
    @Index(name = "idx_alumni_student", columnList = "student_id"),
    @Index(name = "idx_alumni_batch", columnList = "batch_year")
})
public class AlumniRecord extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alumni_id")
    private Long alumniId;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "full_name", length = 200, nullable = false)
    private String fullName;

    @Column(name = "batch_year", length = 10)
    private String batchYear;

    @Column(name = "year_passed", length = 10)
    private String yearPassed;

    @Column(name = "course", length = 100)
    private String course;

    @Column(name = "occupation", length = 100)
    private String occupation;

    @Column(name = "employer", length = 200)
    private String employer;

    @Column(name = "contact", length = 30)
    private String contact;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "graduation_date")
    private LocalDate graduationDate;

    @Column(name = "linked_in", length = 255)
    private String linkedIn;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;
}
