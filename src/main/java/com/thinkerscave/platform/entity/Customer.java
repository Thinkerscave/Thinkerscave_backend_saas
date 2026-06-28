package com.thinkerscave.platform.entity;

import com.thinkerscave.platform.enums.CustomerStatus;
import com.thinkerscave.platform.enums.CustomerType;
import com.thinkerscave.platform.enums.PreferredCommunication;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true, callSuper = false)
@Table(
        name = "customers",
        indexes = {
                @Index(name = "idx_customer_code", columnList = "customer_code"),
                @Index(name = "idx_customer_name", columnList = "display_name"),
                @Index(name = "idx_customer_email", columnList = "email"),
                @Index(name = "idx_customer_mobile", columnList = "mobile_number"),
                @Index(name = "idx_customer_status", columnList = "status")
        }
)
public class Customer extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Business unique customer code.
     * Example : CUS000001
     */
    @Column(name = "customer_code", nullable = false, unique = true, length = 50)
    private String customerCode;

    /**
     * Registered legal name.
     */
    @Column(name = "legal_name", nullable = false, length = 200)
    private String legalName;

    /**
     * Display name used across platform.
     */
    @Column(name = "display_name", nullable = false, length = 200)
    private String displayName;

    /**
     * Customer type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "customer_type", nullable = false, length = 50)
    private CustomerType customerType;

    /**
     * Customer lifecycle status.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CustomerStatus status = CustomerStatus.LEAD;

    /**
     * Primary email.
     */
    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    /**
     * Primary mobile number.
     */
    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    /**
     * Alternate mobile number.
     */
    @Column(name = "alternate_mobile_number", length = 20)
    private String alternateMobileNumber;

    /**
     * Official website.
     */
    @Column(name = "website", length = 255)
    private String website;

    /**
     * GST / VAT / Tax Number.
     */
    @Column(name = "tax_number", length = 100)
    private String taxNumber;

    /**
     * Business registration number.
     */
    @Column(name = "registration_number", length = 100)
    private String registrationNumber;

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
     * Postal / ZIP code.
     */
    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /**
     * Organization logo URL.
     */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /**
     * Preferred communication method.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_communication", length = 30)
    private PreferredCommunication preferredCommunication;

    /**
     * Whether onboarding has been completed.
     */
    @Builder.Default
    @Column(name = "onboarding_completed", nullable = false)
    private Boolean onboardingCompleted = false;

    /**
     * Customer active flag.
     */
    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Internal remarks.
     */
    @Column(name = "remarks", length = 1000)
    private String remarks;

    /**
     * Customer contacts.
     */
    @Builder.Default
    @OneToMany(
            mappedBy = "customer",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<CustomerContact> contacts = new HashSet<>();

    /**
     * Organizations owned by this customer.
     */
    @Builder.Default
    @OneToMany(
            mappedBy = "customer",
            fetch = FetchType.LAZY
    )
    private Set<Organization> organizations = new HashSet<>();

}