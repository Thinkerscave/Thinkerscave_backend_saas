package com.thinkerscave.settings;

import com.thinkerscave.settings.dto.ModuleSettingsRequest;
import com.thinkerscave.settings.dto.ModuleSettingsResponse;
import com.thinkerscave.settings.entity.ModuleSetting;
import com.thinkerscave.settings.repository.ModuleSettingRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.context.TenantContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModuleSettingsService {
    private final ModuleSettingRepository repository;

    public ModuleSettingsResponse getSettings(String requestedModuleCode) {
        String moduleCode = ModuleSettingCatalog.normalizeModule(requestedModuleCode);
        List<ModuleSettingDefinition> definitions = ModuleSettingCatalog.definitionsFor(moduleCode);
        requireOrganizationId();
        Map<String, ModuleSetting> saved = repository.findAllByModuleCode(moduleCode)
                .stream().collect(LinkedHashMap::new,
                        (values, setting) -> values.put(setting.getSettingKey(), setting), Map::putAll);
        Map<String, Object> values = new LinkedHashMap<>();

        for (ModuleSettingDefinition definition : definitions) {
            ModuleSetting setting = saved.get(definition.key());
            values.put(definition.key(), setting == null
                    ? definition.defaultValue()
                    : readStoredValue(definition, setting));
        }

        return new ModuleSettingsResponse(moduleCode, values,
                definitions.stream().filter(ModuleSettingDefinition::editable).toList());
    }

    @Transactional
    public ModuleSettingsResponse saveSettings(String requestedModuleCode, ModuleSettingsRequest request) {
        String moduleCode = ModuleSettingCatalog.normalizeModule(requestedModuleCode);
        List<ModuleSettingDefinition> editable = ModuleSettingCatalog.editableDefinitionsFor(moduleCode);
        if (request == null || request.settings() == null) {
            throw new BadRequestException("Settings are required");
        }
        requireOrganizationId();
        Map<String, ModuleSettingDefinition> allowed = editable.stream()
                .collect(LinkedHashMap::new,
                        (values, definition) -> values.put(definition.key(), definition), Map::putAll);
        Map<String, String> validatedValues = new LinkedHashMap<>();
        request.settings().forEach((requestedKey, rawValue) -> {
            String key = requestedKey == null ? "" : requestedKey.trim().toUpperCase(Locale.ROOT);
            ModuleSettingDefinition definition = allowed.get(key);
            if (definition == null) {
                throw new BadRequestException("Setting key is not editable: " + requestedKey);
            }
            validatedValues.put(key, ModuleSettingCatalog.toStoredValue(definition, rawValue));
        });

        Map<String, ModuleSetting> saved = repository.findAllByModuleCode(moduleCode)
                .stream().collect(LinkedHashMap::new,
                        (values, setting) -> values.put(setting.getSettingKey(), setting), Map::putAll);
        List<ModuleSetting> updates = validatedValues.entrySet().stream().map(entry -> {
            String key = entry.getKey();
            ModuleSettingDefinition definition = allowed.get(key);
            ModuleSetting setting = saved.getOrDefault(key, new ModuleSetting());
            setting.setModuleCode(moduleCode);
            setting.setSettingKey(key);
            setting.setSettingValue(entry.getValue());
            return setting;
        }).toList();

        repository.saveAllAndFlush(updates);
        return getSettings(moduleCode);
    }

    @Transactional
    public ModuleSettingsResponse resetSettings(String requestedModuleCode) {
        String moduleCode = ModuleSettingCatalog.normalizeModule(requestedModuleCode);
        Collection<String> keys = ModuleSettingCatalog.editableDefinitionsFor(moduleCode).stream()
                .map(ModuleSettingDefinition::key).toList();
        requireOrganizationId();
        repository.deleteByModuleCodeAndSettingKeyIn(moduleCode, keys);
        return getSettings(moduleCode);
    }

    public String getString(Long organizationId, String moduleCode, String key) {
        return (String) getValue(organizationId, moduleCode, key);
    }

    public Integer getInteger(Long organizationId, String moduleCode, String key) {
        return (Integer) getValue(organizationId, moduleCode, key);
    }

    public BigDecimal getDecimal(Long organizationId, String moduleCode, String key) {
        return (BigDecimal) getValue(organizationId, moduleCode, key);
    }

    public Boolean getBoolean(Long organizationId, String moduleCode, String key) {
        return (Boolean) getValue(organizationId, moduleCode, key);
    }

    public LocalTime getTime(Long organizationId, String moduleCode, String key) {
        return (LocalTime) getValue(organizationId, moduleCode, key);
    }

    private Object getValue(Long organizationId, String requestedModuleCode, String key) {
        String moduleCode = ModuleSettingCatalog.normalizeModule(requestedModuleCode);
        ModuleSettingDefinition definition = ModuleSettingCatalog.definition(moduleCode, key);
        if (organizationId == null || !organizationId.equals(requireOrganizationId())) {
            throw new BadRequestException("Settings must be read in the current tenant context");
        }
        return repository.findByModuleCodeAndSettingKey(moduleCode, definition.key())
                .map(setting -> readStoredValue(definition, setting))
                .orElse(definition.defaultValue());
    }

    private Object readStoredValue(ModuleSettingDefinition definition, ModuleSetting setting) {
        return ModuleSettingCatalog.fromStoredValue(definition, setting.getSettingValue());
    }

    private Long requireOrganizationId() {
        String tenant = TenantContext.getTenant();
        if (tenant == null || !tenant.startsWith("tenant_")) {
            throw new BadRequestException("A tenant schema is required for module settings");
        }
        Long organizationId = OrganizationContext.getOrganizationId();
        if (organizationId == null) {
            throw new BadRequestException("Organization context is required for settings");
        }
        return organizationId;
    }
}