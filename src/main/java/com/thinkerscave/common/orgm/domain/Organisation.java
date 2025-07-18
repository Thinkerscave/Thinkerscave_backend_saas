package com.thinkerscave.common.orgm.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.usrm.domain.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;

@Data
@Entity
@EntityListeners(AuditingEntityListener.class)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "organisation")
public class Organisation extends Auditable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "org_id")
    private Long orgId;

    @Column(name = "org_code", unique = true, nullable = false, length = 50)
    private String orgCode;

    @Column(name = "org_name", nullable = false, length = 255)
    private String orgName;

    @Column(name = "brand_name", length = 255)
    private String brandName;

    @Column(name = "org_url", length = 255)
    private String orgUrl;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "type", length = 50)
    private String type;

    @Column(name = "city", length = 255)
    private String city;

    @Column(name = "state", length = 255)
    private String state;

//    @Column(name = "created_at", columnDefinition = "TIMESTAMPTZ")
//    private OffsetDateTime createdAt;
//
//    @Column(name = "updated_at", columnDefinition = "TIMESTAMPTZ")
//    private OffsetDateTime updatedAt;

    @Column(name = "created_by", length = 255)
    private String createdBy;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_group")
    private Boolean isGroup = false;
}