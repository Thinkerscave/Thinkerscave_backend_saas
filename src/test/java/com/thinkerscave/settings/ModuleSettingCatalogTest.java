package com.thinkerscave.settings;

import com.thinkerscave.shared.exceptions.BadRequestException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModuleSettingCatalogTest {

    @Test
    void exposesOnlyTheFiveFinalizedAttendanceSettings() {
        List<ModuleSettingDefinition> definitions = ModuleSettingCatalog.editableDefinitionsFor("attendance");

        assertThat(definitions).extracting(ModuleSettingDefinition::key).containsExactly(
                ModuleSettingCatalog.ATTENDANCE_MODE,
                ModuleSettingCatalog.LOW_ATTENDANCE_THRESHOLD,
                ModuleSettingCatalog.ATTENDANCE_EDITING_POLICY,
                ModuleSettingCatalog.STAFF_LATE_GRACE_MINUTES,
                ModuleSettingCatalog.MINIMUM_WORKING_HOURS);
        assertThat(ModuleSettingCatalog.definition("ATTENDANCE", ModuleSettingCatalog.ATTENDANCE_MODE)
                .allowedValues()).containsExactly("DAILY");
    }

    @Test
    void validatesThresholdGracePeriodHoursAndPolicyCodes() {
        ModuleSettingDefinition threshold = ModuleSettingCatalog.definition(
                "ATTENDANCE", ModuleSettingCatalog.LOW_ATTENDANCE_THRESHOLD);
        ModuleSettingDefinition grace = ModuleSettingCatalog.definition(
                "ATTENDANCE", ModuleSettingCatalog.STAFF_LATE_GRACE_MINUTES);
        ModuleSettingDefinition hours = ModuleSettingCatalog.definition(
                "ATTENDANCE", ModuleSettingCatalog.MINIMUM_WORKING_HOURS);
        ModuleSettingDefinition policy = ModuleSettingCatalog.definition(
                "ATTENDANCE", ModuleSettingCatalog.ATTENDANCE_EDITING_POLICY);

        assertThat(ModuleSettingCatalog.validateAndNormalize(threshold, 100)).isEqualTo(100);
        assertThat(ModuleSettingCatalog.validateAndNormalize(grace, 0)).isEqualTo(0);
        assertThat(ModuleSettingCatalog.validateAndNormalize(hours, new BigDecimal("7.5")))
                .isEqualTo(new BigDecimal("7.5"));
        assertThat(ModuleSettingCatalog.validateAndNormalize(policy, "SAME_WEEK")).isEqualTo("SAME_WEEK");
        assertThatThrownBy(() -> ModuleSettingCatalog.validateAndNormalize(threshold, 101))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> ModuleSettingCatalog.validateAndNormalize(grace, -1))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> ModuleSettingCatalog.validateAndNormalize(hours, new BigDecimal("7.25")))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> ModuleSettingCatalog.validateAndNormalize(policy, "LOCKED_AFTER_SUBMISSION"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void rejectsPeriodAttendanceAndUnknownOrHiddenSettingUpdates() {
        ModuleSettingDefinition mode = ModuleSettingCatalog.definition(
                "ATTENDANCE", ModuleSettingCatalog.ATTENDANCE_MODE);

        assertThatThrownBy(() -> ModuleSettingCatalog.validateAndNormalize(mode, "PERIOD_WISE"))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> ModuleSettingCatalog.definition("ATTENDANCE", "SEND_SMS_ON_ABSENT"))
                .isInstanceOf(BadRequestException.class);
        assertThat(ModuleSettingCatalog.definition("ATTENDANCE", ModuleSettingCatalog.ALLOW_COPY_PREVIOUS)
                .editable()).isFalse();
    }
}