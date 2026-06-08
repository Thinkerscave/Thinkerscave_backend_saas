package com.thinkerscave.student.entity;

import com.thinkerscave.access.entity.User;
import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "parent",
        indexes = {
                @Index(name = "idx_parent_code", columnList = "parent_code"),
                @Index(name = "idx_parent_mobile", columnList = "mobile_number"),
                @Index(name = "idx_parent_email", columnList = "email")
        }
)
public class Parent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "parent_id")
    @EqualsAndHashCode.Include
    private Long parentId;

    @Column(name = "parent_code", nullable = false, unique = true, length = 50)
    private String parentCode;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "mobile_number", nullable = false, length = 20)
    private String mobileNumber;

    @Column(name = "alternate_mobile", length = 20)
    private String alternateMobile;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "occupation", length = 100)
    private String occupation;

    @Column(name = "organization_name", length = 150)
    private String organizationName;

    @Column(name = "qualification", length = 100)
    private String qualification;

    @Column(name = "annual_income")
    private Double annualIncome;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
}