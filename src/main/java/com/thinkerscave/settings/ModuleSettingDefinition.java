package com.thinkerscave.settings;

import java.math.BigDecimal;
import java.util.List;

public record ModuleSettingDefinition(
        String moduleCode,
        String key,
        ModuleSettingValueType valueType,
        Object defaultValue,
        BigDecimal minimum,
        BigDecimal maximum,
        List<String> allowedValues,
        boolean editable) {
}