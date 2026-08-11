package com.thinkerscave.platform.entity;

import com.thinkerscave.platform.enums.InstitutionType;
import com.thinkerscave.platform.enums.OrganizationStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(
        name = "organizations",
        indexes = {
                @Index(name = "idx_org_code", columnList = "organization_code"),
                @Index(name = "idx_org_name", columnList = "organization_name"),
                @Index(name = "idx_org_email", columnList = "email"),
                @Index(name = "idx_org_status", columnList = "status"),
                @Index(name = "idx_org_type", columnList = "institution_type")
        }
)
public class Organization extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Business Unique Organization Code.
     * Example : ORG000001
     */
    @Column(name = "organization_code", nullable = false, unique = true, length = 50)
    private String organizationCode;

    /**
     * Customer owning this organization.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * Organization Name.
     */
    @Column(name = "organization_name", nullable = false, length = 200)
    private String organizationName;

    /**
     * Short Name.
     */
    @Column(name = "short_name", length = 100)
    private String shortName;

    /**
     * Institution Type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "institution_type", nullable = false, length = 50)
    private InstitutionType institutionType;

    /**
     * Board / University.
     */
    @Column(name = "board_name", length = 100)
    private String boardName;

    /**
     * Email.
     */
    @Column(name = "email", length = 150)
    private String email;

    /**
     * Mobile Number.
     */
    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    /**
     * Alternate Mobile Number.
     */
    @Column(name = "alternate_mobile_number", length = 20)
    private String alternateMobileNumber;

    /**
     * Website.
     */
    @Column(name = "website", length = 255)
    private String website;

    /**
     * Address Line 1.
     */
    @Column(name = "address_line_1", length = 255)
    private String addressLine1;

    /**
     * Address Line 2.
     */
    @Column(name = "address_line_2", length = 255)
    private String addressLine2;

    /**
     * City.
     */
    @Column(name = "city", length = 100)
    private String city;

    /**
     * State.
     */
    @Column(name = "state", length = 100)
    private String state;

    /**
     * Country.
     */
    @Column(name = "country", length = 100)
    private String country;

    /**
     * Postal Code.
     */
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /**
     * Time Zone.
     */
    @Column(name = "time_zone", length = 100)
    private String timeZone;

    /**
     * Currency.
     */
    @Column(name = "currency", length = 20)
    private String currency;

    /**
     * Language.
     */
    @Column(name = "language", length = 50)
    private String language;

    /**
     * Logo URL.
     */
    @Column(name = "logo_url", columnDefinition = "TEXT")
    private String logoUrl;

    /**
     * Organization Status.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OrganizationStatus status = OrganizationStatus.ACTIVE;

    /**
     * Organization Active Flag.
     */
    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Platform onboarding completed.
     */
    @Builder.Default
    @Column(name = "onboarding_completed", nullable = false)
    private Boolean onboardingCompleted = false;

    /**
     * Internal Remarks.
     */
    @Column(name = "remarks", length = 1000)
    private String remarks;

    /**
     * Tenant Registry.
     */
    @OneToOne(mappedBy = "organization", fetch = FetchType.LAZY)
    private TenantRegistry tenantRegistry;

    /**
     * Domain Mapping.
     */
    @OneToOne(mappedBy = "organization", fetch = FetchType.LAZY)
    private OrganizationDomain organizationDomain;

    /**
     * Platform Configuration.
     */
    @OneToOne(mappedBy = "organization", fetch = FetchType.LAZY)
    private OrganizationConfiguration organizationConfiguration;

    /**
     * Active Subscription.
     */
    @OneToOne(mappedBy = "organization", fetch = FetchType.LAZY)
    private OrganizationSubscription organizationSubscription;

}