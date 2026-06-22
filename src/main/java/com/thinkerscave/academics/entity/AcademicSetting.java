package com.thinkerscave.academics.entity;

import com.thinkerscave.shared.entity.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Table(
        name = "academic_setting",
        indexes = {
                @Index(name = "idx_setting_key", columnList = "setting_key")
        }
)
public class AcademicSetting extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    @EqualsAndHashCode.Include
    private Long settingId;

    @NotBlank
    @Size(max = 100)
    @Column(name = "setting_key", nullable = false, unique = true)
    private String settingKey;

    @NotBlank
    @Size(max = 500)
    @Column(name = "setting_value", nullable = false)
    private String settingValue;

    @Size(max = 50)
    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "remarks", columnDefinition = "TEXT")
    private String remarks;
}