package com.thinkerscave.common.student.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.thinkerscave.common.Address;
import com.thinkerscave.common.usrm.domain.Auditable;

@Entity
@Data
@Table(name = "student")
public class Student extends Auditable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long studentId;

    @Column(nullable = false, length = 50)
    private String firstName;

    @Column(length = 50)
    private String middleName;

    @Column(nullable = false, length = 50)
    private String lastName;

    @Column(length = 50, unique = true)
    private String email;

    @Column(name = "mobile_number", nullable = false)
    private Long mobileNumber;

    @Column(length = 10)
    private String gender;


    @Embedded
    private Address currentAddress;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "country", column = @Column(name = "permanent_country")),
        @AttributeOverride(name = "state", column = @Column(name = "permanent_state")),
        @AttributeOverride(name = "city", column = @Column(name = "permanent_city")),
        @AttributeOverride(name = "zipCode", column = @Column(name = "permanent_zip_code")),
        @AttributeOverride(name = "addressLine", column = @Column(name = "permanent_address_line", columnDefinition = "TEXT"))
    })
    private Address permanentAddress;

    private boolean isSameAddress;
    
    private LocalDate dateOfBirth;

    private LocalDate enrollmentDate;


    //Student Roll Number
    @Column(length = 50, unique = true)
    private String rollNumber;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(length = 255)
    private String photoUrl;

    @ManyToOne
    private ClassEntity classEntity;

    @ManyToOne
    private Section section;

    @OneToOne
    private User user;

    @ManyToOne
    @JoinColumn(name = "guardian_id", nullable = false)
    private Guardian parent;

    private boolean isActive;




}
