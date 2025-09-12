package com.thinkerscave.common.menum.domain;

import java.util.List;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.enums.RoleType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@EntityListeners(AuditingEntityListener.class)
@Entity(name = "role_master")
public class Role extends Auditable {

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "role_name", length = 50, nullable = false)
    private String roleName;

    @Column(name = "role_code", length = 50, unique = true)
    private String roleCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive=true;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "role_type", length = 30)
    private RoleType roleType=RoleType.SCHOOL;
    // Example values: "SCHOOL", "COLLEGE", "UNIVERSITY", "ADMIN"

    // (Optional) to support multi-tenancy in future:
    @Column(name = "organization_id")
    private Long organizationId;
}

