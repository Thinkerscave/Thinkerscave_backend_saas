package com.thinkerscave.settings.repository;

import com.thinkerscave.settings.entity.ModuleSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ModuleSettingRepository extends JpaRepository<ModuleSetting, Long> {
        List<ModuleSetting> findAllByModuleCode(String moduleCode);

        Optional<ModuleSetting> findByModuleCodeAndSettingKey(String moduleCode, String settingKey);

        long deleteByModuleCodeAndSettingKeyIn(String moduleCode, Collection<String> settingKeys);
}