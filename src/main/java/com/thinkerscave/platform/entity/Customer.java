package com.thinkerscave.platform.entity;

import com.thinkerscave.platform.enums.CustomerStatus;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Lightweight account owner in ThinkerCape.
 * Contact people live on {@link CustomerContact}; orgs/billing live elsewhere.
 */
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
                @Index(name = "idx_customer_name", columnList = "customer_name"),
                @Index(name = "idx_customer_email", columnList = "business_email"),
                @Index(name = "idx_customer_mobile", columnList = "mobile_number"),
                @Index(name = "idx_customer_status", columnList = "status"),
                @Index(name = "idx_customer_owner", columnList = "owner_user_id")
        }
)
public class Customer extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    @Column(name = "customer_code", nullable = false, unique = true, length = 50)
    private String customerCode;

    @Column(name = "customer_name", nullable = false, length = 150)
    private String customerName;

    @Column(name = "business_email", nullable = false, unique = true, length = 150)
    private String businessEmail;

    @Column(name = "mobile_number", nullable = false, unique = true, length = 30)
    private String mobileNumber;

    @Column(name = "alternate_mobile_number", length = 30)
    private String alternateMobileNumber;

    @Column(name = "notes", length = 500)
    private String notes;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CustomerStatus status = CustomerStatus.ACTIVE;

    /** Linked Organization Owner user created at customer onboarding. */
    @Column(name = "owner_user_id")
    private Long ownerUserId;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @OneToMany(
            mappedBy = "customer",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private Set<CustomerContact> contacts = new HashSet<>();

    @Builder.Default
    @OneToMany(mappedBy = "customer", fetch = FetchType.LAZY)
    private Set<Organization> organizations = new HashSet<>();

    public void addContact(CustomerContact contact) {
        contacts.add(contact);
        contact.setCustomer(this);
    }
}
