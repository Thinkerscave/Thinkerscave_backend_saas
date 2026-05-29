package com.thinkerscave.common.course.dto;

import com.thinkerscave.common.course.enums.AcademicSettingValueType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcademicSettingDTO {
    private Long settingId;
    private Long organizationId;
    private String settingKey;
    private String settingValue;
    private AcademicSettingValueType valueType;
    private String category;
    private Boolean isActive;
    private String description;
}