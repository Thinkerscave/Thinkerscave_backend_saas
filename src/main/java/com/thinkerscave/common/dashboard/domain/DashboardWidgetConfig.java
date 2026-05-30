package com.thinkerscave.common.dashboard.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "dashboard_widget_config")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardWidgetConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "role_code", nullable = false, length = 64)
    private String roleCode;

    @Column(name = "widget_key", nullable = false, length = 96)
    private String widgetKey;

    @Column(name = "widget_type", nullable = false, length = 40)
    private String widgetType;

    @Column(name = "title", nullable = false, length = 160)
    private String title;

    @Column(name = "subtitle", length = 500)
    private String subtitle;

    @Column(name = "icon", length = 80)
    private String icon;

    @Column(name = "route", length = 220)
    private String route;

    @Column(name = "section_key", nullable = false, length = 40)
    private String sectionKey;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    @Column(name = "max_items")
    private Integer maxItems;

    @Builder.Default
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;
}