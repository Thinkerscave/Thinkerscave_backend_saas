package com.thinkerscave.common.student.domain;

import com.thinkerscave.common.commonModel.Address;
import com.thinkerscave.common.usrm.domain.Auditable;
import com.thinkerscave.common.usrm.domain.User;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "student")
public class Student extends Auditable{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(nullable = false, length = 50,name = "student_id")
    private Long studentId;

    @Column( length = 50,name ="first_name")
    private String firstName;

    @Column(length = 50,name ="middle_name")
    private String middleName;

    @Column( length = 50,name = "last_name")
    private String lastName;

    @Column(length = 50, unique = true,nullable = false)
    private String email;

    @Column(name = "mobile_number", nullable = false)
    private Long mobileNumber;

    @Column(length = 10)
    private String gender;

    private Long age;


    // One-to-one relation for current address
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "current_address_id", referencedColumnName = "id")
    private Address currentAddress;

    // One-to-one relation for permanent address
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "permanent_address_id", referencedColumnName = "id")
    private Address permanentAddress;

    @Column(length = 50, unique = true,name = "is_same_address")
    private boolean isSameAddress;

    @Column(length = 50, unique = true,name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 50, unique = true,name = "enrollment_date")
    private LocalDate enrollmentDate;


    //Student Roll Number
    @Column(length = 50, unique = true,name = "roll_number")
    private String rollNumber;

    @Column(columnDefinition = "TEXT")
    private String remarks;

    @Column(length = 255,name = "photo_url")
    private String photoUrl;

    @Column(length = 255,name = "is_active")
    private boolean isActive;
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





}
