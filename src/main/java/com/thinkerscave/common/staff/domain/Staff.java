package com.thinkerscave.common.staff.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.usrm.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;




@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "staff")
public class Staff extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "staff_id")
    private Long staffId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @Column(name = "first_name", length = 255)
    private String firstName;

    @Column(name = "middle_name", length = 255)
    private String middleName;

    @Column(name = "last_name", length = 255)
    private String lastName;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "mobile_number", nullable = false, unique = true)
    private Long mobileNumber;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(name = "hire_date")
    private LocalDate hireDate;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "is_active")
    private Boolean isActive = true;

    // Optional convenience constructor
    public Staff(User user, Branch branch, Department department, String firstName, String lastName,
                 String email, Long mobileNumber, String gender, LocalDate dateOfBirth,
                 LocalDate hireDate, String photoUrl, String address, Boolean isActive) {
        this.user = user;
        this.branch = branch;
        this.department = department;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.mobileNumber = mobileNumber;
        this.gender = gender;
        this.dateOfBirth = dateOfBirth;
        this.hireDate = hireDate;
        this.photoUrl = photoUrl;
        this.address = address;
        this.isActive = isActive;
    }
}
