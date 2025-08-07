package com.thinkerscave.common.admission.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Entity representing an admission application and its persistent state.
 * <p>
 * This entity contains all fields mapped to the database for storing application details,
 * including applicant information, contact details, status, and audit information.
 *
 * @author Bibekananda Pradhan
 * @since 2023-06-06
 */
@Entity
@Table(name = "application_admission")
@Data
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

    @Column(name = "address", nullable = false)
    private String address;

    @Column(name = "city", nullable = false)
    private String city;

    @Column(name = "state", nullable = false)
    private String state;

    @Column(name = "pincode", nullable = false)
    private String pincode;

    @Column(name = "emergency_contact", nullable = false)
    private String emergencyContact;

    @ElementCollection
    @CollectionTable(name = "uploaded_documents", joinColumns = @JoinColumn(name = "application_id"))
    @Column(name = "document_url")
    private List<String> uploadedDocuments;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ApplicationStatus status;

    @Column(name = "internal_comments", length = 2000)
    private String internalComments;
}
