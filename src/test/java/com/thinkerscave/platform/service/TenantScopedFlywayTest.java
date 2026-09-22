package com.thinkerscave.platform.service;

import com.thinkerscave.platform.entity.TenantRegistry;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

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
}
