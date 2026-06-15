package com.thinkerscave.student.entity;

import com.thinkerscave.common.common.entity.OrganizationScopedEntity;
import com.thinkerscave.student.entity.Student;
import com.thinkerscave.student.entity.StudentEnrollment;
import com.thinkerscave.student.enums.TransferStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Student-initiated transfer / school-leaving certificate workflow.
 */
@Entity
@Table(name = "transfer_request",
        indexes = {
                @Index(name = "idx_transfer_org", columnList = "organization_id"),
                @Index(name = "idx_transfer_student", columnList = "student_id"),
                @Index(name = "idx_transfer_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferRequest extends OrganizationScopedEntity {

    @Column(name = "request_number", nullable = false, length = 32)
    private String requestNumber;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_id", nullable = false)
    private StudentEnrollment enrollment;

    @Column(name = "requested_on", nullable = false)
    private LocalDate requestedOn;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "destination_school", length = 200)
    private String destinationSchool;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TransferStatus status;

    @Column(name = "approved_by_user_id")
    private Long approvedByUserId;

    @Column(name = "approved_on")
    private LocalDate approvedOn;

    @Column(name = "certificate_number", length = 32)
    private String certificateNumber;

    @Column(name = "certificate_issued_on")
    private LocalDate certificateIssuedOn;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
