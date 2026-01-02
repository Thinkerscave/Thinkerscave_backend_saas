package com.thinkerscave.common.orgm.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.shared.enums.OrganizationType;
import com.thinkerscave.common.usrm.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.LocalDate; // Use LocalDate for dates without time

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
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

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 50)
    private OrganizationType type;

    @Column(name = "city", length = 255)
    private String city;

    @Column(name = "state", length = 255)
    private String state;

    @Column(name = "country", length = 100)
    private String country = "India";

    @Column(name = "zip_code", length = 10)
    private String zipCode;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "website", length = 255)
    private String website;

    // --- NEW FIELDS START ---

    @Column(name = "establishment_date")
    private LocalDate establishmentDate;

    @Column(name = "subscription_type", length = 50)
    private String subscriptionType;

    @ManyToOne(fetch = FetchType.LAZY) // This creates the parent-child relationship
    @JoinColumn(name = "parent_org_id")
    private Organisation parentOrganisation;

    @OneToMany(mappedBy = "parentOrganisation", cascade = CascadeType.ALL)
    private List<Organisation> childOrganisations = new ArrayList<>();

    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

    @Column(name = "affiliation", length = 255)
    private String affiliation; // University affiliation for colleges

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "tenant_schema", length = 100)
    private String tenantSchema; // For schema-based multi-tenancy

    @Column(columnDefinition = "TEXT")
    private String description;

    // --- NEW FIELDS END ---

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_group")
    private Boolean isGroup = false;
}