package com.thinkerscave.settings.dto;

import com.thinkerscave.settings.ModuleSettingDefinition;

import java.util.List;
import java.util.Map;

public record ModuleSettingsResponse(
        String moduleCode,
        Map<String, Object> settings,
        List<ModuleSettingDefinition> definitions) {
}