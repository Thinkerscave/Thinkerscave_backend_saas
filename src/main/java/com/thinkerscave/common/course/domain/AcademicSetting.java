package com.thinkerscave.common.course.domain;

import com.thinkerscave.common.auditing.Auditable;
import com.thinkerscave.common.course.enums.AcademicSettingValueType;
import com.thinkerscave.common.orgm.domain.Organisation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "academic_setting", uniqueConstraints = {
        @UniqueConstraint(name = "uk_academic_setting_org_key", columnNames = { "organization_id", "setting_key" })
}, indexes = {
        @Index(name = "idx_academic_setting_org", columnList = "organization_id"),
        @Index(name = "idx_academic_setting_category", columnList = "category")
})
@org.hibernate.annotations.Filter(name = "tenantFilter", condition = "organization_id = :tenantId")
@Getter
@Setter
public class AcademicSetting extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long settingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organisation organization;

    @Column(name = "setting_key", nullable = false, length = 120)
    private String settingKey;

    @Column(name = "setting_value", columnDefinition = "TEXT")
    private String settingValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 20)
    private AcademicSettingValueType valueType = AcademicSettingValueType.TEXT;

    @Column(name = "category", nullable = false, length = 80)
    private String category = "GENERAL";

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String description;
}