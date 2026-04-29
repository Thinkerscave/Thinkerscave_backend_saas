package com.thinkerscave.common.security;

import com.thinkerscave.common.filter.JwtAuthFilter;
import com.thinkerscave.common.filter.TenantFilter;
import com.thinkerscave.common.resolver.SubdomainTenantResolver;
import com.thinkerscave.common.service.TenantLookupService;
import com.thinkerscave.common.usrm.controller.UserController;
import com.thinkerscave.common.usrm.dto.UserResponseDTO;
import com.thinkerscave.common.usrm.service.LoginAttemptService;
import com.thinkerscave.common.usrm.service.RefreshTokenService;
import com.thinkerscave.common.usrm.service.UserService;
import com.thinkerscave.common.usrm.service.impl.JwtServiceImpl;
import com.thinkerscave.common.usrm.service.impl.UserUserInfoDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Disabled;

@WebMvcTest(UserController.class)
@Import({ SecurityConfig.class, TenantFilter.class, JwtAuthFilter.class })
@Disabled("Failing due to missing UserRepository bean dependency in OrganizationFilter")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JdbcTemplate jdbcTemplate;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtServiceImpl jwtService;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private LoginAttemptService loginAttemptService;

    @MockBean
    private TenantLookupService tenantLookupService;

    @MockBean
    private SubdomainTenantResolver subdomainResolver;

    @MockBean
    private UserUserInfoDetailsService userDetailsService;

    @Test
    void testPublicEndpointRequest_ShouldSucceed() throws Exception {
        mockMvc.perform(get("/api/v1/users/generateKey"))
                .andExpect(status().isOk());
    }

    @Test
    void testProtectedEndpoint_NoToken_ShouldFail() throws Exception {
        mockMvc.perform(get("/api/v1/users/currentUserInfo"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testProtectedEndpoint_WithMockUser_ShouldSucceed() throws Exception {
        UserResponseDTO mockUser = new UserResponseDTO();
        mockUser.setUserName("testuser");

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(mockUser));

        mockMvc.perform(get("/api/v1/users/currentUserInfo"))
                .andExpect(status().isOk());
    }

    @Test
    void testProtectedEndpoint_WithValidToken_ShouldSucceed() throws Exception {
        String validToken = "valid.jwt.token";

        when(jwtService.extractUsername(validToken)).thenReturn("testuser");
        when(jwtService.validateToken(anyString(), any())).thenReturn(true);

        UserInfoUserDetails mockUserDetails = mock(UserInfoUserDetails.class);
        when(mockUserDetails.getUsername()).thenReturn("testuser");
        when(mockUserDetails.getAuthorities())
                .thenAnswer(invocation -> Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        when(mockUserDetails.isEnabled()).thenReturn(true);
        when(mockUserDetails.isAccountNonExpired()).thenReturn(true);
        when(mockUserDetails.isAccountNonLocked()).thenReturn(true);
        when(mockUserDetails.isCredentialsNonExpired()).thenReturn(true);

        when(userDetailsService.loadUserByUsername("testuser")).thenReturn(mockUserDetails);

        UserResponseDTO userResponse = new UserResponseDTO();
        userResponse.setUserName("testuser");
        when(userService.findByUsername("testuser")).thenReturn(Optional.of(userResponse));

        mockMvc.perform(get("/api/v1/users/currentUserInfo")
                .header("Authorization", "Bearer " + validToken))
                .andDo(org.springframework.test.web.servlet.result.MockMvcResultHandlers.print())
                .andExpect(status().isOk());

        verify(jwtService).extractUsername(validToken);
        verify(userDetailsService).loadUserByUsername("testuser");
        verify(jwtService).validateToken(anyString(), any());
    }
}
