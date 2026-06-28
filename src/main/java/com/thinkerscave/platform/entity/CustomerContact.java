package com.thinkerscave.platform.entity;

import com.thinkerscave.platform.enums.ContactType;
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
        name = "customer_contacts",
        indexes = {
                @Index(name = "idx_customer_contact_code", columnList = "contact_code"),
                @Index(name = "idx_customer_contact_email", columnList = "email"),
                @Index(name = "idx_customer_contact_mobile", columnList = "mobile_number"),
                @Index(name = "idx_customer_contact_type", columnList = "contact_type")
        }
)
public class CustomerContact extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    /**
     * Business unique contact code.
     * Example : CON000001
     */
    @Column(name = "contact_code", nullable = false, unique = true, length = 50)
    private String contactCode;

    /**
     * Customer reference.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /**
     * Full name.
     */
    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    /**
     * Designation.
     */
    @Column(name = "designation", length = 100)
    private String designation;

    /**
     * Contact type.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 50)
    private ContactType contactType;

    /**
     * Primary email.
     */
    @Column(name = "email", length = 150)
    private String email;

    /**
     * Mobile number.
     */
    @Column(name = "mobile_number", length = 20)
    private String mobileNumber;

    /**
     * Alternate mobile number.
     */
    @Column(name = "alternate_mobile_number", length = 20)
    private String alternateMobileNumber;

    /**
     * Office phone.
     */
    @Column(name = "office_phone", length = 20)
    private String officePhone;

    /**
     * Department.
     */
    @Column(name = "department", length = 100)
    private String department;

    /**
     * Whether this is the primary contact.
     */
    @Builder.Default
    @Column(name = "primary_contact", nullable = false)
    private Boolean primaryContact = false;

    /**
     * Receives billing communications.
     */
    @Builder.Default
    @Column(name = "billing_contact", nullable = false)
    private Boolean billingContact = false;

    /**
     * Receives technical communications.
     */
    @Builder.Default
    @Column(name = "technical_contact", nullable = false)
    private Boolean technicalContact = false;

    /**
     * Receives sales communications.
     */
    @Builder.Default
    @Column(name = "sales_contact", nullable = false)
    private Boolean salesContact = false;

    /**
     * Receives support communications.
     */
    @Builder.Default
    @Column(name = "support_contact", nullable = false)
    private Boolean supportContact = false;

    /**
     * Active flag.
     */
    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /**
     * Internal remarks.
     */
    @Column(name = "remarks", length = 1000)
    private String remarks;

}