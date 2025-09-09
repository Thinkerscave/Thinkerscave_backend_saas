package com.thinkerscave.common.orgm.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.usrm.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDate; // Use LocalDate for dates without time

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "organisation")
public class Organisation extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "org_code", unique = true, nullable = false, length = 50)
    private String orgCode;

    @Column(name = "org_name", nullable = false, length = 255)
    private String orgName;

    @Column(name = "brand_name", length = 255)
    private String brandName;

    @Column(name = "org_url", length = 255)
    private String orgUrl;

    @ManyToOne(fetch = FetchType.LAZY) // Link to the owner
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "city", length = 255)
    private String city;

    @Column(name = "state", length = 255)
    private String state;

    // --- NEW FIELDS START ---

    @Column(name = "establishment_date")
    private LocalDate establishmentDate;

    @Column(name = "subscription_type", length = 50)
    private String subscriptionType;

    @ManyToOne(fetch = FetchType.LAZY) // This creates the parent-child relationship
    @JoinColumn(name = "parent_org_id")
    private Organisation parentOrganisation;

    // --- NEW FIELDS END ---

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_group")
    private Boolean isGroup = false;
}