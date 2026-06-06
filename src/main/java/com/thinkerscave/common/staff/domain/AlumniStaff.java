package com.thinkerscave.common.staff.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "staff_alumni", indexes = {
        @Index(name = "idx_staff_alumni_org", columnList = "organization_id"),
        @Index(name = "idx_staff_alumni_exit_type", columnList = "exit_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlumniStaff extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alumni_staff_id")
    private Long alumniStaffId;

    @Column(name = "organization_id", nullable = false)
    private Long organizationId;

    @Column(name = "staff_id")
    private Long staffId;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(name = "staff_code", length = 50)
    private String staffCode;

    @Column(name = "last_designation", length = 120)
    private String lastDesignation;

    @Column(name = "department", length = 120)
    private String department;

    @Column(name = "exit_type", nullable = false, length = 40)
    private String exitType;

    @Column(name = "exit_date", nullable = false)
    private LocalDate exitDate;

    @Column(name = "joined_date")
    private LocalDate joinedDate;

    @Column(name = "years_of_service")
    private Double yearsOfService;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "contact", length = 30)
    private String contact;

    @Column(name = "remarks", length = 500)
    private String remarks;
}
