package com.thinkerscave.common.menum.domain;

import com.thinkerscave.common.auditing.Auditable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "sub_menu_master")
public class SubMenu extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sub_menu_id")
    @EqualsAndHashCode.Include
    private Long subMenuId;

    @Column(name = "sub_menu_name", nullable = false, length = 100)
    private String subMenuName;

    @Column(name = "sub_menu_code", nullable = false, unique = true, length = 50)
    private String subMenuCode;

    @Column(name = "sub_menu_description", columnDefinition = "TEXT")
    private String subMenuDescription;

    @Column(name = "sub_menu_url", length = 200)
    private String subMenuUrl;

    @Column(name = "sub_menu_icon", length = 100)
    private String subMenuIcon;

    @Column(name = "sub_menu_order")
    private Integer subMenuOrder;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    private Menu menu;
}
