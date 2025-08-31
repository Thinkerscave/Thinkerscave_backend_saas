package com.thinkerscave.common.admission.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "application_admission")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class ApplicationAdmission extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "application_id", nullable = false, unique = true)
    private String applicationId;

    @Column(name = "applicant_name", nullable = false)
    private String applicantName;

    // ... other fields like dateOfBirth, gender, etc. remain the same ...
    @Column(name = "date_of_birth", nullable = false)
    private LocalDateTime dateOfBirth;

    @Column(name = "gender", nullable = false)
    private String gender;

    @Column(name = "applying_for_school_or_college", nullable = false)
    private String applyingForSchoolOrCollege;

    @Column(name = "parent_name", nullable = false)
    private String parentName;

    @Column(name = "guardian_name")
    private String guardianName;

    @Column(name = "contact_number", nullable = false)
    private String contactNumber;

    @Column(name = "email", nullable = false)
    private String email;

    // --- MODIFICATION START ---

    // Remove the old flat address fields:
    // private String address;
    // private String city;
    // private String state;
    // private String pincode;

    // Replace them with an @Embedded Address object
    @Embedded
    private Address address;

    // Remove the old emergency_contact string
    // private String emergencyContact;

    // Replace it with an @Embedded EmergencyContact object
    @Embedded
    private EmergencyContact emergencyContact;

    // --- MODIFICATION END ---

    @ElementCollection
    @CollectionTable(name = "application_documents", joinColumns = @JoinColumn(name = "application_admission_id"))
    @Column(name = "document_url")
    private List<String> uploadedDocuments;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status;

    @Column(name = "internal_comments", length = 2000)
    private String internalComments;
}