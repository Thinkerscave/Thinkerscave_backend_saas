package com.thinkerscave.academics.security;

import com.thinkerscave.access.repository.UserRepository;
import com.thinkerscave.access.service.PermissionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcademicsAccessGuardStudentTest {

    @Mock private PermissionService permissionService;
    @Mock private UserRepository userRepository;

    private AcademicsAccessGuard guard;

    @BeforeEach
    void setUp() {
        guard = new AcademicsAccessGuard(permissionService, userRepository);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "student@example.test", null, List.of(new SimpleGrantedAuthority("STUDENT"))));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void allowsStudentsToViewOnlyTheirSelfServiceAcademicsResources() {
        assertDoesNotThrow(() -> guard.requireView(AcademicsAccessGuard.RESOURCE_MY_TIMETABLE));
        assertDoesNotThrow(() -> guard.requireView(AcademicsAccessGuard.RESOURCE_MY_ACADEMICS));
        verifyNoPermissionLookup();
    }

    @Test
    void keepsAdministrativeTimetableResourcesPermissionGated() {
        when(userRepository.findByUsername("student@example.test")).thenReturn(Optional.empty());

        assertThrows(AccessDeniedException.class,
                () -> guard.requireView(AcademicsAccessGuard.RESOURCE_TIMETABLE));
        verify(permissionService, never()).hasPermission(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    private void verifyNoPermissionLookup() {
        verify(permissionService, never()).hasPermission(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}
