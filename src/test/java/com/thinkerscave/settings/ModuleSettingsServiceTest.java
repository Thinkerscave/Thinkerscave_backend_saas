package com.thinkerscave.settings;

import com.thinkerscave.settings.dto.ModuleSettingsRequest;
import com.thinkerscave.settings.entity.ModuleSetting;
import com.thinkerscave.settings.repository.ModuleSettingRepository;
import com.thinkerscave.shared.context.OrganizationContext;
import com.thinkerscave.shared.context.TenantContext;
import com.thinkerscave.shared.exceptions.BadRequestException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModuleSettingsServiceTest {
    private static final Long ORGANIZATION_ID = 42L;

    @Mock
    private ModuleSettingRepository repository;

        @BeforeEach
        void setTenantContext() {
                TenantContext.setTenant("tenant_settings_test");
        }

    @AfterEach
    void clearOrganizationContext() {
        OrganizationContext.clear();
        TenantContext.clear();
    }

    @Test
    void savesAndReloadsSettingsWithinCurrentOrganization() {
        OrganizationContext.setOrganizationId(ORGANIZATION_ID);
        Map<String, ModuleSetting> stored = new HashMap<>();
        when(repository.findAllByModuleCode(ModuleSettingCatalog.ATTENDANCE))
                .thenAnswer(invocation -> new ArrayList<>(stored.values()));
        when(repository.saveAllAndFlush(anyList())).thenAnswer(invocation -> {
            List<ModuleSetting> updates = invocation.getArgument(0);
            updates.forEach(setting -> stored.put(setting.getSettingKey(), setting));
            return updates;
        });

        ModuleSettingsService service = new ModuleSettingsService(repository);
        var response = service.saveSettings("attendance", new ModuleSettingsRequest(Map.of(
                ModuleSettingCatalog.LOW_ATTENDANCE_THRESHOLD, 88,
                ModuleSettingCatalog.ATTENDANCE_EDITING_POLICY, "SAME_MONTH")));

        assertThat(response.settings())
                .containsEntry(ModuleSettingCatalog.LOW_ATTENDANCE_THRESHOLD, 88)
                .containsEntry(ModuleSettingCatalog.ATTENDANCE_EDITING_POLICY, "SAME_MONTH");
        assertThat(response.definitions()).hasSize(5);
        verify(repository).saveAllAndFlush(anyList());
        verify(repository, org.mockito.Mockito.atLeastOnce())
                .findAllByModuleCode(ModuleSettingCatalog.ATTENDANCE);
    }

    @Test
    void returnsCatalogDefaultsWhenOrganizationHasNoSavedRows() {
        OrganizationContext.setOrganizationId(ORGANIZATION_ID);
        when(repository.findAllByModuleCode(ModuleSettingCatalog.ATTENDANCE))
                .thenReturn(List.of());

        var response = new ModuleSettingsService(repository).getSettings("ATTENDANCE");

        assertThat(response.settings())
                .containsEntry(ModuleSettingCatalog.ATTENDANCE_MODE, "DAILY")
                .containsEntry(ModuleSettingCatalog.LOW_ATTENDANCE_THRESHOLD, 75)
                .containsEntry(ModuleSettingCatalog.ATTENDANCE_EDITING_POLICY, "SAME_DAY")
                .containsEntry(ModuleSettingCatalog.STAFF_LATE_GRACE_MINUTES, 15)
                .containsEntry(ModuleSettingCatalog.MINIMUM_WORKING_HOURS, new java.math.BigDecimal("8"));
        verify(repository).findAllByModuleCode(ModuleSettingCatalog.ATTENDANCE);
    }

    @Test
    void readsSavedValuesOnlyForRequestedOrganization() {
        OrganizationContext.setOrganizationId(ORGANIZATION_ID);
        ModuleSetting saved = new ModuleSetting();
        saved.setModuleCode(ModuleSettingCatalog.ATTENDANCE);
        saved.setSettingKey(ModuleSettingCatalog.ATTENDANCE_EDITING_POLICY);
        saved.setSettingValue("SAME_WEEK");
        when(repository.findByModuleCodeAndSettingKey(
                ModuleSettingCatalog.ATTENDANCE, ModuleSettingCatalog.ATTENDANCE_EDITING_POLICY))
                .thenReturn(Optional.of(saved));

        ModuleSettingsService service = new ModuleSettingsService(repository);

        assertThat(service.getString(ORGANIZATION_ID, ModuleSettingCatalog.ATTENDANCE,
                ModuleSettingCatalog.ATTENDANCE_EDITING_POLICY)).isEqualTo("SAME_WEEK");
        assertThatThrownBy(() -> service.getString(43L, ModuleSettingCatalog.ATTENDANCE,
                ModuleSettingCatalog.ATTENDANCE_EDITING_POLICY)).isInstanceOf(BadRequestException.class);
        verify(repository).findByModuleCodeAndSettingKey(
                ModuleSettingCatalog.ATTENDANCE, ModuleSettingCatalog.ATTENDANCE_EDITING_POLICY);
    }

    @Test
    void rejectsHiddenAndUnknownKeysBeforePersistence() {
        OrganizationContext.setOrganizationId(ORGANIZATION_ID);
        ModuleSettingsService service = new ModuleSettingsService(repository);

        assertThatThrownBy(() -> service.saveSettings("ATTENDANCE",
                new ModuleSettingsRequest(Map.of(ModuleSettingCatalog.ALLOW_COPY_PREVIOUS, false))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not editable");
        assertThatThrownBy(() -> service.saveSettings("ATTENDANCE",
                new ModuleSettingsRequest(Map.of("UNREGISTERED_KEY", "anything"))))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not editable");
        verifyNoInteractions(repository);
    }

        @Test
        void rejectsPublicSchemaBeforeRepositoryAccess() {
                OrganizationContext.setOrganizationId(ORGANIZATION_ID);
                TenantContext.setTenant("public");
                assertThatThrownBy(() -> new ModuleSettingsService(repository).getSettings("ATTENDANCE"))
                                .isInstanceOf(BadRequestException.class).hasMessageContaining("tenant schema");
                verifyNoInteractions(repository);
        }
}