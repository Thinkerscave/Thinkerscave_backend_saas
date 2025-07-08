package com.thinkerscave.common.menum.domain;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@EntityListeners(AuditingEntityListener.class)
@Entity
@Table(name = "submenu_master")
public class Submenu extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String submenuName;

    @Column(nullable = false, unique = true)
    private String submenuCode;

    private String url;
    private String icon;
    private Integer sequence;
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;

    // Optional Fields
    private String tooltip;
    private String componentName;
    private String permissionKey;
    private Boolean isVisible = true;
    private Boolean deleted = false;

    // Getters and Setters
}