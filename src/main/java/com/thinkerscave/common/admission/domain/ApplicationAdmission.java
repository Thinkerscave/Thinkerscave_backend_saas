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
    @Column(name = "application_id", unique = true)
    private String applicationId;

    @Column(name = "applicant_name")
    private String applicantName;

    // ... other fields like dateOfBirth, gender, etc. remain the same ...
    @Column(name = "date_of_birth")
    private LocalDateTime dateOfBirth;

    @Column(name = "gender")
    private String gender;

    @Column(name = "applying_for_school_or_college")
    private String applyingForSchoolOrCollege;

    @Column(name = "parent_name")
    private String parentName;

    @Column(name = "guardian_name")
    private String guardianName;

    @Column(name = "contact_number")
    private String contactNumber;

    @Column(name = "email")
    private String email;

    // --- MODIFICATION START ---

    // Remove the old flat address fields:
    // private String address;
    // private String city;
    // private String state;
    // private String pincode;

    // Replace them with an @Embedded Address object
    @Embedded
    private AddressEmbedded address;

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
    @Column(name = "status")
    private ApplicationStatus status;

    @Column(name = "internal_comments", length = 2000)
    private String internalComments;

    @Column(name = "organization_id")
    private Long organizationId;
}