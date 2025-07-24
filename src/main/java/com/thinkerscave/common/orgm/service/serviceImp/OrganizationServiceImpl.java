package com.thinkerscave.common.orgm.service.serviceImp;
import com.thinkerscave.common.config.TenantContext;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.domain.OwnerDetails;
import com.thinkerscave.common.orgm.dto.OrgRequestDTO;
import com.thinkerscave.common.orgm.dto.OrgResponseDTO;
import com.thinkerscave.common.orgm.dto.OwnerDTO;
import com.thinkerscave.common.orgm.repository.OrganizationRepository;
import com.thinkerscave.common.orgm.repository.OwnerDetailsRepository;
import com.thinkerscave.common.orgm.service.OrganizationService;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of OrganizationService to manage organization creation, updates, and soft deletions.
 * Handles user and owner detail linkage during organization registration.
 *
 * @author Sandeep
 */
@Service
@RequiredArgsConstructor
public class OrganizationServiceImpl implements OrganizationService {

    private static final Logger logger = LoggerFactory.getLogger(OrganizationServiceImpl.class);

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final OwnerDetailsRepository ownerDetailsRepository;

    /** Saves organization along with user and owner details. */
    @Override
    @Transactional
    public OrgResponseDTO saveOrganization(OrgRequestDTO request) {
        String schema = TenantContext.getTenant();
        if (schema == null) {
            throw new IllegalStateException("Tenant (schema) not set. Please provide 'X-Tenant-ID' in the header.");
        }

        // Step 1: Handle User
        logger.info("👤 [User] Preparing user data for: {}", request.getName());
        String[] names = request.getName().split(" ", 3);
        User user = new User();
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

        // Step 2: Create New Organization
        Organisation organisation = new Organisation();
        organisation.setOrgCode("ORG" + UUID.randomUUID().toString().substring(0, 8));
        organisation.setOrgName(request.getOrganizationName());
        organisation.setBrandName(request.getBrandName());
        organisation.setType(request.getOrgType());
        organisation.setCity(request.getCity());
        organisation.setState(request.getState());
        organisation.setCreatedBy(savedUser.getUserName());
        organisation.setUser(savedUser);

        Organisation savedOrg = organizationRepository.save(organisation);

        /// Step 3: Save Owner Details
        logger.info("👑 [Owner] Creating owner details...");
        OwnerDetails owner = new OwnerDetails();
        owner.setOwnerCode("OWNR" + UUID.randomUUID().toString().substring(0, 8)); // 👈 generate owner code
        owner.setGender(request.getGender());
        owner.setMailId(request.getMailId());
        owner.setUser(savedUser);
        owner.setOrganization(savedOrg);
        ownerDetailsRepository.save(owner);

        return new OrgResponseDTO(
                "Organization successfully registered under tenant: " + schema,
                savedOrg.getOrgCode(),
                savedUser.getUserCode()
        );
    }


    /** Returns all organizations. */
    @Override
    public List<Organisation> getAllOrgs() {
        return organizationRepository.findAll();
    }

    /** Soft-deletes an organization by marking it inactive. */
    @Override
    @Transactional
    public String softDeleteOrg(String orgCode) {
        Optional<Organisation> optionalOrg = organizationRepository.findByOrgCode(orgCode);

        if (optionalOrg.isEmpty()) {
            return "Organization not found.";
        }

        Organisation org = optionalOrg.get();

        if (Boolean.FALSE.equals(org.getIsActive())) {
            return "Organization already inactive.";
        }

        org.setIsActive(false);
        organizationRepository.save(org);
        return "Organization soft-deleted (marked inactive) successfully.";
    }

    public void updateOwnerDetailsWithUser(OwnerDTO request) {
        OwnerDetails owner = ownerDetailsRepository.findByOwnerCode(request.getOwnerCode())
                .orElseThrow(() -> new RuntimeException("Owner not found with code: " + request.getOwnerCode()));

        // Update OwnerDetails
        owner.setOwnerName(request.getOwnerName());
        owner.setGender(request.getGender());
        owner.setMailId(request.getMailId());

        // Update linked User entity
        User user = owner.getUser();
        user.setUserName(request.getUserName() != null ? request.getUserName() : user.getUserName());
        user.setAddress(request.getAddress() != null ? request.getAddress() : user.getAddress());
        user.setMobileNumber(request.getPhoneNumber() != null ? request.getPhoneNumber() : user.getMobileNumber());
        user.setCity(request.getCity() != null ? request.getCity() : user.getCity());
        user.setState(request.getState() != null ? request.getState() : user.getState());


        // Save updates
        userRepository.save(user);
        ownerDetailsRepository.save(owner);
    }




    /** Generates a unique username using name and timestamp. */
    private String generateUniqueUserName(String fullName) {
        return fullName.toLowerCase().replace(" ", "_") + "_" + System.currentTimeMillis() % 10000;
    }

    /** Generates a random 10-character password. */
    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 10);
    }
}

