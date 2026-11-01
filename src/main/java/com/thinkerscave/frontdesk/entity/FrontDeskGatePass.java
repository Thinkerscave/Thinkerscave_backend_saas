package com.thinkerscave.frontdesk.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@Table(name = "front_desk_gate_pass")
public class FrontDeskGatePass extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gate_pass_id")
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", nullable = false, length = 200)
    private String studentName;

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(name = "section_name", length = 50)
    private String sectionName;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "pickup_person", nullable = false, length = 150)
    private String pickupPerson;

    @Column(name = "verification_mode", nullable = false, length = 20)
    private String verificationMode;

    @Column(name = "attendance_status", nullable = false, length = 30)
    private String attendanceStatus;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;
}
