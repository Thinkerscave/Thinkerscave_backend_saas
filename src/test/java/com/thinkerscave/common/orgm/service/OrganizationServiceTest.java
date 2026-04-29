package com.thinkerscave.common.orgm.service;

import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.domain.OwnerDetails;
import com.thinkerscave.common.orgm.dto.*;
import com.thinkerscave.common.orgm.repository.OrganizationRepository;
import com.thinkerscave.common.orgm.repository.OwnerDetailsRepository;
import com.thinkerscave.common.orgm.service.serviceImp.OrganizationServiceImpl;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private OwnerDetailsRepository ownerDetailsRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private OrganizationServiceImpl organizationService;

    private OrgRequestDTO validRequest;
    private User mockUser;
    private Organisation mockOrg;
    private OwnerDetails mockOwner;

    @BeforeEach
    void setUp() {
        validRequest = OrgRequestDTO.builder()
                .orgName("Test Org")
                .ownerName("Test Owner")
                .ownerEmail("owner@test.com")
                .ownerMobile("1234567890")
                .isAGroup(false)
                .build();

        mockUser = new User();
        mockUser.setUserName("test_owner_123");
        mockUser.setUserCode("USER-123");
        mockUser.setEmail("owner@test.com");

        mockOrg = new Organisation();
        mockOrg.setOrgName("Test Org");
        mockOrg.setOrgCode("ORG-123");
        mockOrg.setUser(mockUser);

        mockOwner = new OwnerDetails();
        mockOwner.setOwnerName("Test Owner");
        mockOwner.setUser(mockUser);
        mockOwner.setOrganization(mockOrg);
    }

    @Test
    void saveOrganization_NewUser_Success() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenReturn(mockUser);
        when(organizationRepository.save(any(Organisation.class))).thenReturn(mockOrg);
        when(ownerDetailsRepository.save(any(OwnerDetails.class))).thenReturn(mockOwner);

        OrgResponseDTO response = organizationService.saveOrganization(validRequest);

        assertNotNull(response);
        assertEquals("ORG-123", response.getOrgCode());
        assertEquals("USER-123", response.getUserCode());
        assertNotNull(response.getInitialPassword()); // Should return generated password for new users

        verify(userRepository, times(1)).save(any(User.class));
        verify(organizationRepository, times(1)).save(any(Organisation.class));
        verify(ownerDetailsRepository, times(1)).save(any(OwnerDetails.class));
    }

    @Test
    void saveOrganization_ExistingUser_Success() {
        when(userRepository.findByEmail(validRequest.getOwnerEmail())).thenReturn(Optional.of(mockUser));
        when(organizationRepository.save(any(Organisation.class))).thenReturn(mockOrg);
        when(ownerDetailsRepository.save(any(OwnerDetails.class))).thenReturn(mockOwner);

        OrgResponseDTO response = organizationService.saveOrganization(validRequest);

        assertNotNull(response);
        assertEquals("ORG-123", response.getOrgCode());
        assertEquals("USER-123", response.getUserCode());
        assertNull(response.getInitialPassword()); // No password returned for existing users

        verify(userRepository, never()).save(any(User.class)); // Shouldn't create new user
        verify(organizationRepository, times(1)).save(any(Organisation.class));
        verify(ownerDetailsRepository, times(1)).save(any(OwnerDetails.class));
    }

    @Test
    void getAllOrgsAsDTO_Success() {
        when(organizationRepository.findAll()).thenReturn(Collections.singletonList(mockOrg));

        var result = organizationService.getAllOrgsAsDTO();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Org", result.get(0).getOrgName());
    }
}
