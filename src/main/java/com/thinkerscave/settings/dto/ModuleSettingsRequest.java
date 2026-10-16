package com.thinkerscave.settings.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ModuleSettingsRequest(@NotNull Map<String, Object> settings) {
}