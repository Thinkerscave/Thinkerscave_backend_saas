package com.thinkerscave.platform.service;

import com.thinkerscave.platform.entity.TenantRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class TenantScopedFlywayTest {
    private final TenantScopedFlyway flyway = new TenantScopedFlyway(mock(DataSource.class));

    @Test
    void rejectsSchemaThatIsNotFromTrustedTenantNamespace() {
        TenantRegistry tenant = TenantRegistry.builder().id(12L).schemaName("public").build();

        assertThatThrownBy(() -> flyway.currentVersion(tenant))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("trusted tenant schema");
    }

    @Test
    void rejectsDetachedTenantRegistry() {
        TenantRegistry tenant = TenantRegistry.builder().schemaName("tenant_school").build();

        assertThatThrownBy(() -> flyway.migrate(tenant, "2"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void requiresExplicitReleaseTarget() {
        TenantRegistry tenant = TenantRegistry.builder().id(1L).schemaName("tenant_school").build();

        assertThatThrownBy(() -> flyway.migrate(tenant, " "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("target version");
    }

    @Test
    void latestTargetIncludesLegacyTimetableConstraintCorrection() {
        assertThat(TenantScopedFlyway.LATEST_VERSION).isEqualTo("48");
    }

    @Test
    void v48DropsTheLegacyFullSlotConstraint() throws IOException {
        try (var migration = getClass().getClassLoader().getResourceAsStream(
                "db/migration/tenant/V48__remove_legacy_timetable_slot_constraint.sql")) {
            assertThat(migration).isNotNull();
            assertThat(new String(migration.readAllBytes(), StandardCharsets.UTF_8))
                    .contains("DROP CONSTRAINT IF EXISTS "
                            + "timetable_entry_timetable_version_id_section_id_day_of_week_key");
        }
    }
}
