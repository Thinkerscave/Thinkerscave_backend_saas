package com.thinkerscave.student.entity;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.shared.entity.Address;
import com.thinkerscave.shared.entity.Auditable;
import com.thinkerscave.student.enums.StudentStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "student",
        indexes = {
                @Index(name = "idx_student_code", columnList = "student_code"),
                @Index(name = "idx_student_admission_no", columnList = "admission_number"),
                @Index(name = "idx_student_email", columnList = "email"),
                @Index(name = "idx_student_mobile", columnList = "mobile_number"),
                @Index(name = "idx_student_status", columnList = "status")
        }
)
public class Student extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "student_code", nullable = false, unique = true, length = 50)
    @EqualsAndHashCode.Include
    private String studentCode;

    @Column(name = "admission_number", nullable = false, unique = true, length = 50)
    private String admissionNumber;

    @Column(name = "roll_number", length = 50)
    private String rollNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "religion", length = 50)
    private String religion;

    @Column(name = "nationality", length = 50)
    private String nationality;

    @Column(name = "mother_tongue", length = 50)
    private String motherTongue;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "place_of_birth", length = 120)
    private String placeOfBirth;

    @Column(name = "identity_document_type", length = 50)
    private String identityDocumentType;

    @Column(name = "identity_document_number", length = 80)
    private String identityDocumentNumber;

    @Column(name = "application_id")
    private Long applicationId;

    @Column(name = "mobile_number")
    private Long mobileNumber;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "admission_date")
    private LocalDate admissionDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private StudentStatus status = StudentStatus.ACTIVE;

    @Column(name = "transport_required")
    private Boolean transportRequired = false;

    @Column(name = "hostel_required")
    private Boolean hostelRequired = false;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "current_address_id")
    private Address currentAddress;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "permanent_address_id")
    private Address permanentAddress;

    @Column(name = "same_address")
    private Boolean sameAddress = false;

    @Column(name = "emergency_contact_name", length = 150)
    private String emergencyContactName;

    @Column(name = "emergency_contact_phone", length = 30)
    private String emergencyContactPhone;

    @Column(name = "emergency_contact_relation", length = 50)
    private String emergencyContactRelation;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}
