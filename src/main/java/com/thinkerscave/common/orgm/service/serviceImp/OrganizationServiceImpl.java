package com.thinkerscave.common.orgm.service.serviceImp;


import com.thinkerscave.common.orgm.config.TenantContext;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.domain.OwnerDetails;
import com.thinkerscave.common.orgm.repository.OrganizationRepository;
import com.thinkerscave.common.orgm.repository.OwnerDetailsRepository;
import com.thinkerscave.common.orgm.requestDto.OrgRegistrationRequest;
import com.thinkerscave.common.orgm.responseDto.OrgRegistrationResponse;
import com.thinkerscave.common.orgm.service.OrganizationService;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OwnerDetailsRepository ownerDetailsRepository;

    @Override
    @Transactional
    public OrgRegistrationResponse registerOrg(OrgRegistrationRequest request) {

        // ✅ Step 0: Get Tenant (schema) from context
        String schema = TenantContext.getTenant();
        if (schema == null) {
            throw new IllegalStateException("Tenant (schema) not set. Please provide 'X-Tenant-ID' in the header.");
        }
        System.out.println("📦 [TenantContext] Active schema: " + schema);

        // ✅ Step 1: Create User
        System.out.println("👤 [User] Creating user with name: " + request.getName());
        User user = new User();
        String[] names = request.getName().split(" ", 3);
        user.setFirstName(names[0]);
        user.setMiddleName(names.length == 3 ? names[1] : null);
        user.setLastName(names.length >= 2 ? names[names.length - 1] : "");

        user.setEmail(request.getMailId());
        user.setMobileNumber(request.getPhoneNumber());
        user.setUserName(generateUniqueUserName(request.getName()));
        user.setPassword(generateRandomPassword());
        user.setAddress(request.getAddress());
        user.setCity(request.getCity());
        user.setState(request.getState());
        user.setUserCode("USER" + UUID.randomUUID().toString().substring(0, 8));

        User savedUser = userRepository.save(user);
        System.out.println("✅ [User] Saved user: " + savedUser.getUserCode());

        // ✅ Step 2: Create Organization
        System.out.println("🏢 [Organization] Creating organization: " + request.getOrganizationName());
        Organisation organisation = new Organisation();
        organisation.setOrgName(request.getOrganizationName());
        organisation.setBrandName(request.getBrandName());
        organisation.setType(request.getOrgType());
        organisation.setCity(request.getCity());
        organisation.setState(request.getState());
        organisation.setCreatedAt(OffsetDateTime.now());
        organisation.setCreatedBy(user.getUserName());
        organisation.setUser(savedUser);
        organisation.setOrgCode("ORG" + UUID.randomUUID().toString().substring(0, 8));

        Organisation savedOrg = organizationRepository.save(organisation);
        System.out.println("✅ [Organization] Saved org: " + savedOrg.getOrgCode());

        // ✅ Step 3: Create OwnerDetails
        System.out.println("👑 [Owner] Creating owner details...");
        OwnerDetails owner = new OwnerDetails();
        owner.setGender(request.getGender());
        owner.setMailId(request.getMailId());
        owner.setUser(savedUser);
        owner.setOrganization(savedOrg);

        ownerDetailsRepository.save(owner);
        System.out.println("✅ [Owner] Saved owner for user: " + savedUser.getUserName());

        // ✅ Step 4: Return response
        System.out.println("🚀 [Registration] Completed for tenant: " + schema);
        return new OrgRegistrationResponse(
                "Organization successfully registered under tenant: " + schema,
                savedOrg.getOrgCode(),
                savedUser.getUserCode()
        );
    }


    private String generateUniqueUserName(String fullName) {
        return fullName.toLowerCase().replace(" ", "_") + "_" + System.currentTimeMillis() % 10000;
    }

    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 10);
    }
}

