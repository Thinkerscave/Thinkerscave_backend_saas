package com.thinkerscave.common.orgm.service.serviceImp;
import com.thinkerscave.common.config.TenantContext;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.domain.OwnerDetails;
import com.thinkerscave.common.orgm.dto.OrgRequestDTO;
import com.thinkerscave.common.orgm.dto.OrgResponseDTO;
import com.thinkerscave.common.orgm.dto.OrganisationListDTO;
import com.thinkerscave.common.orgm.dto.OwnerDTO;
import com.thinkerscave.common.orgm.repository.OrganizationRepository;
import com.thinkerscave.common.orgm.repository.OwnerDetailsRepository;
import com.thinkerscave.common.orgm.service.OrganizationService;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final PasswordEncoder passwordEncoder;

    /** Saves organization along with user and owner details. */
//    @Override
//    @Transactional
//    public OrgResponseDTO saveOrganization(OrgRequestDTO request) {
//        String schema = TenantContext.getTenant();
//        if (schema == null) {
//            throw new IllegalStateException("Tenant (schema) not set. Please provide 'X-Tenant-ID' in the header.");
//        }
//
//        // Step 1: Handle User
//        logger.info("👤 [User] Preparing user data for: {}", request.getName());
//        String[] names = request.getName().split(" ", 3);
//        User user = new User();
//        user.setFirstName(names[0]);
//        user.setMiddleName(names.length == 3 ? names[1] : null);
//        user.setLastName(names.length >= 2 ? names[names.length - 1] : "");
//        user.setEmail(request.getMailId());
//        user.setMobileNumber(request.getPhoneNumber());
//        user.setUserName(generateUniqueUserName(request.getName()));
//        user.setPassword(generateRandomPassword());
//        user.setAddress(request.getAddress());
//        user.setCity(request.getCity());
//        user.setState(request.getState());
//        user.setUserCode("USER" + UUID.randomUUID().toString().substring(0, 8));
//
//        User savedUser = userRepository.save(user);
//
//        // Step 2: Create New Organization
//        Organisation organisation = new Organisation();
//        organisation.setOrgCode("ORG" + UUID.randomUUID().toString().substring(0, 8));
//        organisation.setOrgName(request.getOrganizationName());
//        organisation.setBrandName(request.getBrandName());
//        organisation.setType(request.getOrgType());
//        organisation.setCity(request.getCity());
//        organisation.setState(request.getState());
//        organisation.setCreatedBy(savedUser.getUserName());
//        organisation.setUser(savedUser);
//
//        Organisation savedOrg = organizationRepository.save(organisation);
//
//        /// Step 3: Save Owner Details
//        logger.info("👑 [Owner] Creating owner details...");
//        OwnerDetails owner = new OwnerDetails();
//        owner.setOwnerCode("OWNR" + UUID.randomUUID().toString().substring(0, 8)); // 👈 generate owner code
//        owner.setGender(request.getGender());
//        owner.setMailId(request.getMailId());
//        owner.setUser(savedUser);
//        owner.setOrganization(savedOrg);
//        ownerDetailsRepository.save(owner);
//
//        return new OrgResponseDTO(
//                "Organization successfully registered under tenant: " + schema,
//                savedOrg.getOrgCode(),
//                savedUser.getUserCode()
//        );
//    }

    @Override
    @Transactional // Ensures the entire operation succeeds or fails together
    public OrgResponseDTO saveOrganization(OrgRequestDTO request) {
        String schema = TenantContext.getTenant();
        if (schema == null) {
            throw new IllegalStateException("Tenant (schema) not set. Please provide 'X-Tenant-ID' in the header.");
        }

        // Step 1: Find an existing user or create a new one.
        User savedUser = findOrCreateUser(request);

        // Step 2: Create the new Organisation, handling the parent relationship.
        Organisation savedOrg = createOrganisation(request, savedUser);

        // Step 3: Create the OwnerDetails to link the User and Organisation.
        createOwnerDetails(request, savedUser, savedOrg);

        // TODO: Consider sending a welcome email to the user with their generated username
        // and instructions to use the "Forgot Password" feature to set their password.

        return new OrgResponseDTO(
                "Organization successfully registered under tenant: " + schema,
                savedOrg.getOrgCode(),
                savedUser.getUserCode()
        );
    }

    /**
     * Fetches all organizations and converts them to a safe DTO format for the API.
     * This is the method your controller should now call.
     */
    @Transactional(readOnly = true) // Use a read-only transaction for performance
    public List<OrganisationListDTO> getAllOrgsAsDTO() {
        return organizationRepository.findAll()
                .stream()
                .map(this::toOrganisationListDTO) // Use a helper to map each entity
                .collect(Collectors.toList());
    }

    /**
     * A private helper method to safely map an Organisation entity to its DTO representation.
     * This explicitly calls the getters, which resolves the lazy-loaded proxies.
     */
    private OrganisationListDTO toOrganisationListDTO(Organisation org) {
        OrganisationListDTO dto = new OrganisationListDTO();

        dto.setOrgId(org.getOrgId());
        dto.setOrgName(org.getOrgName());
        dto.setBrandName(org.getBrandName());
        dto.setOrgUrl(org.getOrgUrl());
        dto.setOrgType(org.getType());
        dto.setCity(org.getCity());
        dto.setState(org.getState());
        dto.setEstablishDate(org.getEstablishmentDate());
        dto.setGroup(org.getIsGroup());

        // --- THIS IS THE FIX ---
        // Safely access lazy-loaded fields to get the real data.
        if (org.getParentOrganisation() != null) {
            dto.setParentOrgId(org.getParentOrganisation().getOrgId());
        }

        if (org.getUser() != null) {
            dto.setOwnerName(org.getUser().getFirstName()); // Or getFullName() if you have it
            dto.setOwnerEmail(org.getUser().getEmail());
            dto.setOwnerMobile(String.valueOf(org.getUser().getMobileNumber()));
        }

        return dto;
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
        owner.setOwnerEmail(request.getMailId());

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


    /**
     * Finds a user by email. If the user doesn't exist, creates a new one.
     */
    private User findOrCreateUser(OrgRequestDTO request) {
        logger.info("👤 [User] Checking for existing user with email: {}", request.getOwnerEmail());

        return userRepository.findByEmail(request.getOwnerEmail())
                .orElseGet(() -> {
                    logger.info("👤 [User] No existing user found. Creating a new user.");
                    User newUser = new User();
                    newUser.setFirstName(request.getOwnerName()); // Assume ownerName is the full name for simplicity
                    newUser.setLastName(""); // Can be enhanced to split name
                    newUser.setEmail(request.getOwnerEmail());
                    newUser.setMobileNumber(Long.valueOf(request.getOwnerMobile()));
                    newUser.setUserName(generateUniqueUserName(request.getOwnerName()));

                    // --- SECURITY FIX: Hash the password before saving ---
                    String rawPassword = generateRandomPassword();
                    newUser.setPassword(passwordEncoder.encode(rawPassword));

                    newUser.setUserCode("USER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    // Set default roles, active status, etc. here if needed

                    return userRepository.save(newUser);
                });
    }

    /**
     * Creates and saves a new Organisation entity from the request DTO.
     */
    private Organisation createOrganisation(OrgRequestDTO request, User ownerUser) {
        logger.info("🏢 [Organisation] Creating new organisation: {}", request.getOrgName());
        Organisation organisation = new Organisation();
        organisation.setOrgCode("ORG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        organisation.setOrgName(request.getOrgName());
        organisation.setBrandName(request.getBrandName());
        organisation.setType(request.getOrgType());
        organisation.setCity(request.getCity());
        organisation.setState(request.getState());
        organisation.setEstablishmentDate(request.getEstablishDate());
        organisation.setSubscriptionType(request.getSubscriptionType());

        // --- HIERARCHY LOGIC ---
        organisation.setIsGroup(request.getIsAGroup());
        if (!request.getIsAGroup() && request.getParentOrgId() != null) {
            Organisation parent = organizationRepository.findById(request.getParentOrgId())
                    .orElseThrow(() -> new RuntimeException("Parent organization with ID " + request.getParentOrgId() + " not found."));
            organisation.setParentOrganisation(parent);
        }

        organisation.setUser(ownerUser); // Link the owner (as a user)

        return organizationRepository.save(organisation);
    }

    /**
     * Creates and saves the OwnerDetails entity to link the User and Organisation.
     */
    private void createOwnerDetails(OrgRequestDTO request, User user, Organisation org) {
        logger.info("👑 [Owner] Creating owner details for user {} and org {}", user.getUserName(), org.getOrgName());
        OwnerDetails owner = new OwnerDetails();
        owner.setOwnerCode("OWNR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        owner.setOwnerName(request.getOwnerName());
        owner.setOwnerEmail(request.getOwnerEmail());
        owner.setOwnerMobile(request.getOwnerMobile());
//        owner.setGender(request.getGender());
        owner.setUser(user);
        owner.setOrganization(org);
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

