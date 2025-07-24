package com.thinkerscave.common.menum.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
@Table(name = "menu_master")
public class Menu extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Long menuId;

    @Column(name="menu_code")
    private String menuCode;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 55)
    private String url;

    @Column(length = 55)
    private String icon;

    @Column(name = "\"order\"")
    private Integer order;

    @Column(name = "is_active")
    private Boolean isActive;


   }
