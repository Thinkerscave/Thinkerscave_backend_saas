package com.thinkerscave.common.usrm.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.menum.domain.Role;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;


@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "users")
public class User extends Auditable {
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

    @Column(name = "user_code", unique = true, nullable = false)
    private String userCode;

    @Column(name = "first_name", nullable = false, length = 255)
    private String firstName;

    @Column(name = "middle_name", length = 255)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 255)
    private String lastName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "mobile_number", nullable = false)
    private Long mobileNumber;

    @Column(name = "user_name", unique = true, nullable = false, length = 255)
    private String userName;

    @Column(name = "password", nullable = false, length = 60)
    private String password;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "city", length = 255)
    private String city;

    @Column(name = "state", length = 255)
    private String state;

//    @Column(name = "parent_user_name", length = 255)
//    private String parentUserName;

    @Column(name = "is_blocked")
    private Boolean isBlocked = false;

    @Column(name = "is_2fa_enabled")
    private Boolean is2faEnabled = false;

    @Column(name = "max_device_allow")
    private Integer maxDeviceAllow;

    @Column(name = "attempts")
    private Integer attempts = 0;

    @Column(name = "lock_date_time")
    private LocalDateTime lockDateTime;

    @Column(name = "secret_operation", length = 255)
    private String secretOperation;

    @Column(name = "remarks", length = 255)
    private String remarks;
    
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private List<Role> roles = new ArrayList<>();

    
    // other Optional fields:

    public User(String userName, String email, String password, String firstName, String middleName, String lastName,
            String address, String city, String state, Long mobileNumber,
            Boolean isBlocked, Boolean is2faEnabled, Integer maxDeviceAllow, Integer attempts,
            LocalDateTime lockDateTime, String secretOperation, String remarks) {

    this.userName = userName;
    this.email = email;
    this.password = password;
    this.firstName = firstName;
    this.middleName = middleName;
    this.lastName = lastName;
    this.address = address;
    this.city = city;
    this.state = state;
    this.mobileNumber = mobileNumber;
    this.isBlocked = isBlocked;
    this.is2faEnabled = is2faEnabled;
    this.maxDeviceAllow = maxDeviceAllow;
    this.attempts = attempts;
    this.lockDateTime = lockDateTime;
    this.secretOperation = secretOperation;
    this.remarks = remarks;
}
}
