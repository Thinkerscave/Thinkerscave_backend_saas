package com.thinkerscave.frontdesk.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalDateTime;


@Entity
@Getter
@Setter
@Table(name = "front_desk_certificate_request")
public class FrontDeskCertificateRequest extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long id;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "student_name", nullable = false, length = 200)
    private String studentName;

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(name = "roll_number", length = 50)
    private String rollNumber;

    @Column(name = "certificate_type", nullable = false, length = 40)
    private String certificateType;

    @Column(name = "issuance_status", nullable = false, length = 30)
    private String issuanceStatus;

    @Column(name = "printed_on")
    private LocalDateTime printedOn;

    @Column(name = "tc_number", length = 80)
    private String tcNumber;

    @Column(name = "tc_issued_on")
    private LocalDate tcIssuedOn;
}
