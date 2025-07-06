package com.thinkerscave.common.student.domain;

import com.thinkerscave.common.usrm.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import com.thinkerscave.common.commonModel.Address;
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


    // One-to-one relation for current address
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "current_address_id", referencedColumnName = "id")
    private Address currentAddress;

    // One-to-one relation for permanent address
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "permanent_address_id", referencedColumnName = "id")
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
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id")
    private Section section;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;


    @ManyToOne
    @JoinColumn(name = "guardian_id", nullable = false)
    private Guardian parent;

    private boolean isActive;



}
