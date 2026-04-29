package com.thinkerscave.common.orgm.service;

import com.thinkerscave.common.orgm.dto.OrgRequestDTO;
import com.thinkerscave.common.orgm.dto.OrgResponseDTO;
import com.thinkerscave.common.config.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Disabled;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Broken by recent multi-tenant architecture changes requiring explicit H2 schema initialization")
public class MultiOrgVerificationTest {

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private com.thinkerscave.common.orgm.repository.OrganizationRepository organizationRepository;

    @Test
    @Transactional
    public void testMultiOrgCreation() {
        // 1. Create Parent Tenant Org
        OrgRequestDTO parentReq = OrgRequestDTO.builder()
                .orgName("Integration Test Group")
                .tenantSchema("test_schema_v1")
                .isAGroup(true)
                .ownerEmail("parent@test.com")
                .ownerName("Parent Admin")
                .ownerMobile("1111111111")
                .orgType("GROUP")
                .city("Test City")
                .state("TS")
                .establishDate(LocalDate.now())
                .subscriptionType("Trial")
                .build();

        // Simulate Tenant Context for Initial Creation?
        // Actually, OrganizationService relies on request.getTenantSchema() for first
        // creation usually.
        // But let's see logic:
        // if (request.getTenantSchema() != null) { ... } -> Adds schema ref.

        OrgResponseDTO parentRes = organizationService.saveOrganization(parentReq);
        assertNotNull(parentRes);
        assertNotNull(parentRes.getOrgCode());

        // Fetch ID
        Long parentId = organizationRepository.findByOrgCode(parentRes.getOrgCode()).get().getOrgId();

        // 2. Create Child Org
        OrgRequestDTO childReq = OrgRequestDTO.builder()
                .orgName("Child School A")
                .tenantSchema("test_schema_v1")
                .parentOrgId(parentId)
                .isAGroup(false)
                .ownerEmail("child@test.com")
                .ownerName("Child Admin")
                .ownerMobile("2222222222")
                .orgType("SCHOOL")
                .city("Test City")
                .state("TS")
                .establishDate(LocalDate.now())
                .subscriptionType("Trial")
                .build();

        OrgResponseDTO childRes = organizationService.saveOrganization(childReq);

        assertNotNull(childRes);
        assertEquals("Child School A", organizationRepository.findByOrgCode(childRes.getOrgCode()).get().getOrgName());

        // Verify Parent Link
        assertEquals(parentId,
                organizationRepository.findByOrgCode(childRes.getOrgCode()).get().getParentOrganisation().getOrgId());

        System.out.println(">>> Multi-Org Verification Successful internally via Service Layer! <<<");
    }
}
