package com.thinkerscave.common.orgm.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.usrm.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "owner_details")
public class OwnerDetails extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "owner_code",   unique = true, length = 50)
    private String ownerCode;

    @Column(name = "owner_name",   length = 100)
    private String ownerName;

    @Column(name = "gender",   length = 10)
    private String gender;

    // --- ADJUSTED: Renamed for consistency ---
    @Column(name = "owner_email",   length = 255)
    private String ownerEmail;

    // --- NEW FIELD ---
    @Column(name = "owner_mobile", length = 20)
    private String ownerMobile;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "org_id", nullable = false)
    private Organisation organization;
}