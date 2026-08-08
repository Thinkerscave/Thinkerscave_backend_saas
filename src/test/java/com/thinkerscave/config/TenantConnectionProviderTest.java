package com.thinkerscave.config;

import com.thinkerscave.config.tenancy.TenantSchemaSwitcher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantConnectionProvider fail-closed schema switch")
class TenantConnectionProviderTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private TenantSchemaSwitcher schemaSwitcher;
    @Mock
    private Connection connection;

    @InjectMocks
    private TenantConnectionProvider provider;

    @Test
    @DisplayName("returns connection after successful tenant switch")
    void returnsConnectionOnSuccess() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);

        Connection result = provider.getConnection("tenant_abc");

        assertSame(connection, result);
        verify(schemaSwitcher).switchToTenant(connection, "tenant_abc");
        verify(connection, never()).close();
    }

    @Test
    @DisplayName("closes connection and rethrows when schema switch fails (no platform fallback)")
    void failsClosedOnSwitchError() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(schemaSwitcher.resolvePhysicalSchema("tenant_missing")).thenReturn("tenant_missing");
        doThrow(new SQLException("Unknown database")).when(schemaSwitcher)
                .switchToTenant(connection, "tenant_missing");

        SQLException thrown = assertThrows(SQLException.class,
                () -> provider.getConnection("tenant_missing"));

        assertEquals("Unknown database", thrown.getMessage());
        verify(connection).close();
    }
}
