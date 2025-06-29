package com.thinkerscave.common.orgm.domain;

import com.thinkerscave.common.usrm.domain.Auditable;
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

    @Column(name = "gender", nullable = false, length = 10)
    private String gender;

    @Column(name = "mail_id", nullable = false, length = 255)
    private String mailId;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToOne
    @JoinColumn(name = "org_id", nullable = false)
    private Organisation organization;


}
