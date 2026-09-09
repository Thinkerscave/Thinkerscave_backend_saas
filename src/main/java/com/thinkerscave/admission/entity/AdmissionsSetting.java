package com.thinkerscave.admission.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "admissions_setting",
        indexes = {
                @Index(name = "idx_adm_setting_org", columnList = "organization_id", unique = true)
        }
)
public class AdmissionsSetting extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    @EqualsAndHashCode.Include
    private Long settingId;

    @Column(name = "organization_id", nullable = false, unique = true)
    private Long organizationId;

    @Column(name = "inquiry_sources", columnDefinition = "TEXT")
    private String inquirySources;

    @Column(name = "inquiry_statuses", columnDefinition = "TEXT")
    private String inquiryStatuses;

    @Column(name = "required_documents", columnDefinition = "TEXT")
    private String requiredDocuments;

    @Column(name = "lead_prefix", length = 20)
    private String leadPrefix = "LD";

    @Column(name = "application_prefix", length = 20)
    private String applicationPrefix = "APP";

    @Column(name = "admission_prefix", length = 20)
    private String admissionPrefix = "ADM";

    @Column(name = "reminder_mode", length = 30)
    private String reminderMode = "AUTO";

    @Column(name = "reminder_lead_time", length = 20)
    private String reminderLeadTime = "24H";

    @Column(name = "assignment_mode", length = 30)
    private String assignmentMode = "MANUAL";

    /** Monotonic cursor used for sequential round-robin counselor assignment. */
    @Column(name = "next_counselor_index", nullable = false)
    private Long nextCounselorIndex = 0L;
}
