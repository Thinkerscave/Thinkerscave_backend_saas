package com.thinkerscave.common.role.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.menum.domain.Menu;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.*;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "role_menu",
            joinColumns = @JoinColumn(name = "role_id"),
            inverseJoinColumns = @JoinColumn(name = "menu_id"))
    private List<Menu> menus = new ArrayList<>();


    @PrePersist
    public void prePersist() {
        if (this.isActive == null) {
            this.isActive = false;
        }
        if (this.roleCode == null || this.roleCode.isBlank()) {
            if (this.roleName != null && !this.roleName.isBlank()) {
                this.roleCode = "ROLE_" + roleName.trim().toUpperCase().replace(" ", "_");
            } else {
                this.roleCode = "ROLE_" + UUID.randomUUID().toString().replace("-", "")
                                             .substring(0, 6).toUpperCase();
            }
        }
    }



}
