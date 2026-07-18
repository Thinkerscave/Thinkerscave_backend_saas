package com.thinkerscave.platform.entity;

import com.thinkerscave.platform.enums.ContactType;
import com.thinkerscave.platform.enums.ContactTypeConverter;
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
                @Index(name = "idx_customer_contact_customer", columnList = "customer_id"),
                @Index(name = "idx_customer_contact_type", columnList = "contact_type"),
                @Index(name = "idx_customer_contact_email", columnList = "email")
        }
)
public class CustomerContact extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id")
    private Long id;

    @Column(name = "contact_code", nullable = false, unique = true, length = 50)
    private String contactCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Convert(converter = ContactTypeConverter.class)
    @Column(name = "contact_type", nullable = false, length = 30)
    private ContactType contactType;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "mobile_number", length = 30)
    private String mobileNumber;

    @Column(name = "designation", length = 100)
    private String designation;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;
}
