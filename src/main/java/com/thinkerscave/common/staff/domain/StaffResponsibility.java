package com.thinkerscave.common.staff.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "staff_responsibility", indexes = {
        @Index(name = "idx_staff_resp_org", columnList = "organization_id"),
        @Index(name = "idx_staff_resp_staff", columnList = "staff_id"),
        @Index(name = "idx_staff_resp_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StaffResponsibility extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "responsibility_id")
    private Long responsibilityId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "responsibility_name", nullable = false, length = 150)
    private String responsibilityName;

    @Column(name = "responsibility_type", nullable = false, length = 40)
    private String responsibilityType;

    @Column(name = "scope", length = 120)
    private String scope;

    @Column(name = "effective_from")
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 20)
    private String status = "ASSIGNED";

    @Column(name = "remarks", length = 500)
    private String remarks;
}
