package com.thinkerscave.platform.controller;

import com.thinkerscave.platform.dto.response.TenantMigrationResponse;
import com.thinkerscave.platform.enums.OperationStatus;
import com.thinkerscave.platform.service.TenantMigrationService;
import com.thinkerscave.platform.service.TenantService;
import com.thinkerscave.shared.dto.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantRegistryControllerTest {
    @Mock private TenantService tenantService;
    @Mock private TenantMigrationService tenantMigrationService;
    @InjectMocks private TenantRegistryController controller;

    @Test
    void reportsMigrationFailureInsteadOfReturningSuccess() {
        when(tenantMigrationService.executeTenant(1L, "49"))
                .thenReturn(new TenantMigrationResponse(
                        1L, null, 1L, "tenant_test", "48", "49", "48",
                        OperationStatus.FAILED, 1, null, null, "Validation failed"));

        ResponseEntity<ApiResponse<?>> response = controller.triggerMigration(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        verify(tenantMigrationService).executeTenant(1L, "49");
    }
}
