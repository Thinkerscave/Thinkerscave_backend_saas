package com.thinkerscave.staff.entity;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.shared.entity.Auditable;
import com.thinkerscave.staff.enums.EmploymentCategory;
import com.thinkerscave.staff.enums.EmploymentStatus;
import com.thinkerscave.staff.enums.StaffType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "staff",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_staff_code",
                        columnNames = "staff_code"
                ),
                @UniqueConstraint(
                        name = "uk_staff_email",
                        columnNames = "email"
                )
        },
        indexes = {
                @Index(name = "idx_staff_code", columnList = "staff_code"),
                @Index(name = "idx_staff_type", columnList = "staff_type"),
                @Index(name = "idx_staff_status", columnList = "employment_status"),
                @Index(name = "idx_staff_joining_date", columnList = "joining_date")
        }
)
public class Staff extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    @EqualsAndHashCode.Include
    private Long staffId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @NotBlank
    @Size(max = 30)
    @Column(name = "staff_code", nullable = false, length = 30)
    private String staffCode;

    @NotBlank
    @Size(max = 100)
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Size(max = 100)
    @Column(name = "middle_name", length = 100)
    private String middleName;

    @NotBlank
    @Size(max = 100)
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @NotBlank
    @Size(max = 20)
    @Column(name = "gender", nullable = false, length = 20)
    private String gender;

    @NotNull
    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Size(max = 10)
    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Size(max = 50)
    @Column(name = "religion", length = 50)
    private String religion;

    @Size(max = 50)
    @Column(name = "nationality", length = 50)
    private String nationality;

    @NotBlank
    @Size(max = 15)
    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @Email
    @Size(max = 150)
    @Column(name = "email", nullable = false, length = 150)
    private String email;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "staff_type", nullable = false, length = 30)
    private StaffType staffType;

    @NotBlank
    @Size(max = 100)
    @Column(name = "designation", nullable = false, length = 100)
    private String designation;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "employment_category", nullable = false, length = 30)
    private EmploymentCategory employmentCategory;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "employment_status", nullable = false, length = 30)
    private EmploymentStatus employmentStatus = EmploymentStatus.ACTIVE;

    @NotNull
    @Column(name = "joining_date", nullable = false)
    private LocalDate joiningDate;

    @Column(name = "contract_start_date")
    private LocalDate contractStartDate;

    @Column(name = "contract_end_date")
    private LocalDate contractEndDate;

    @Size(max = 255)
    @Column(name = "highest_qualification")
    private String highestQualification;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Size(max = 150)
    @Column(name = "emergency_contact_name")
    private String emergencyContactName;

    @Size(max = 100)
    @Column(name = "emergency_contact_relation")
    private String emergencyContactRelation;

    @Size(max = 15)
    @Column(name = "emergency_contact_number")
    private String emergencyContactNumber;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}