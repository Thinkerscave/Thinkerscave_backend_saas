package com.thinkerscave.common.orgm.service.serviceImp;

//import com.thinkerscave.common.config.TenantContext;
import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.shared.enums.OrganizationType;
import com.thinkerscave.common.orgm.domain.OwnerDetails;
import com.thinkerscave.common.orgm.dto.*;
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
 * Implementation of OrganizationService to manage organization creation,
 * updates, and soft deletions.
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
    // @Override
    // @Transactional
    // public OrgResponseDTO saveOrganization(OrgRequestDTO request) {
    // String schema = TenantContext.getTenant();
    // if (schema == null) {
    // throw new IllegalStateException("Tenant (schema) not set. Please provide
    // 'X-Tenant-ID' in the header.");
    // }
    //
    // // Step 1: Handle User
    // logger.info("👤 [User] Preparing user data for: {}", request.getName());
    // String[] names = request.getName().split(" ", 3);
    // User user = new User();
    // user.setFirstName(names[0]);
    // user.setMiddleName(names.length == 3 ? names[1] : null);
    // user.setLastName(names.length >= 2 ? names[names.length - 1] : "");
    // user.setEmail(request.getMailId());
    // user.setMobileNumber(request.getPhoneNumber());
    // user.setUserName(generateUniqueUserName(request.getName()));
    // user.setPassword(generateRandomPassword());
    // user.setAddress(request.getAddress());
    // user.setCity(request.getCity());
    // user.setState(request.getState());
    // user.setUserCode("USER" + UUID.randomUUID().toString().substring(0, 8));
    //
    // User savedUser = userRepository.save(user);
    //
    // // Step 2: Create New Organization
    // Organisation organisation = new Organisation();
    // organisation.setOrgCode("ORG" + UUID.randomUUID().toString().substring(0,
    // 8));
    // organisation.setOrgName(request.getOrganizationName());
    // organisation.setBrandName(request.getBrandName());
    // organisation.setType(request.getOrgType());
    // organisation.setCity(request.getCity());
    // organisation.setState(request.getState());
    // organisation.setCreatedBy(savedUser.getUserName());
    // organisation.setUser(savedUser);
    //
    // Organisation savedOrg = organizationRepository.save(organisation);
    //
    // /// Step 3: Save Owner Details
    // logger.info("👑 [Owner] Creating owner details...");
    // OwnerDetails owner = new OwnerDetails();
    // owner.setOwnerCode("OWNR" + UUID.randomUUID().toString().substring(0, 8)); //
    // 👈 generate owner code
    // owner.setGender(request.getGender());
    // owner.setMailId(request.getMailId());
    // owner.setUser(savedUser);
    // owner.setOrganization(savedOrg);
    // ownerDetailsRepository.save(owner);
    //
    // return new OrgResponseDTO(
    // "Organization successfully registered under tenant: " + schema,
    // savedOrg.getOrgCode(),
    // savedUser.getUserCode()
    // );
    // }

    @Override
    @Transactional
    public OrgResponseDTO saveOrganization(OrgRequestDTO request) {
        // String schema = TenantContext.getTenant();
        // if (schema == null) {
        // throw new IllegalStateException("Tenant (schema) not set. Please provide
        // 'X-Tenant-ID' in the header.");
        // }

        // Step 1: Find an existing user or create a new one.
        // User savedUser = findOrCreateUser(request);

        String initialPassword = null;
        User savedUser = userRepository.findByEmail(request.getOwnerEmail()).orElse(null);

        if (savedUser == null) {
            // Only create a user in public schema if NOT called from tenant onboarding
            // (tenant onboarding already created the user in the tenant schema)
            if (request.getTenantSchema() != null && !request.getTenantSchema().isEmpty()) {
                // Called from tenant onboarding — create a minimal public-schema user
                // with the SAME password (already hashed by the caller)
                logger.info("👤 [User] Tenant onboarding mode — creating linked public user for: {}",
                        request.getOwnerEmail());
                User newUser = new User();
                newUser.setFirstName(request.getOwnerName() != null ? request.getOwnerName() : "Admin");
                newUser.setLastName("");
                newUser.setEmail(request.getOwnerEmail());
                Long mobileNum = null;
                if (request.getOwnerMobile() != null && !request.getOwnerMobile().trim().isEmpty()) {
                    try {
                        mobileNum = Long.valueOf(request.getOwnerMobile().trim());
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid mobile number format: {}", request.getOwnerMobile());
                        throw new IllegalArgumentException("Invalid mobile number format: " + request.getOwnerMobile());
                    }
                }
                newUser.setMobileNumber(mobileNum);
                newUser.setUserName(request.getOwnerEmail()); // Use email as username for consistency
                newUser.setPassword("TENANT_MANAGED"); // Password is managed in tenant schema
                newUser.setUserCode("USER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                savedUser = userRepository.save(newUser);
            } else {
                // Standalone org registration — create a new user with a random password
                logger.info("👤 [User] No existing user found. Creating a new user.");
                User newUser = new User();
                newUser.setFirstName(request.getOwnerName());
                newUser.setLastName("");
                newUser.setEmail(request.getOwnerEmail());
                Long mobileNum = null;
                if (request.getOwnerMobile() != null && !request.getOwnerMobile().trim().isEmpty()) {
                    try {
                        mobileNum = Long.valueOf(request.getOwnerMobile().trim());
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid mobile number format for user {}: {}",
                                request.getOwnerName(), request.getOwnerMobile());
                        throw new IllegalArgumentException("Invalid mobile number format: " + request.getOwnerMobile());
                    }
                }
                newUser.setMobileNumber(mobileNum);
                newUser.setUserName(generateUniqueUserName(request.getOwnerName()));

                // --- SECURITY FIX: Hash the password before saving ---
                String rawPassword = generateRandomPassword();
                initialPassword = rawPassword; // Capture for response
                newUser.setPassword(passwordEncoder.encode(rawPassword));

                newUser.setUserCode("USER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                savedUser = userRepository.save(newUser);
            }
        }

        // Step 2: Create the new Organisation, handling the parent relationship.
        Organisation savedOrg = createOrganisation(request, savedUser);

        // Step 3: Create the OwnerDetails to link the User and Organisation.
        createOwnerDetails(request, savedUser, savedOrg);

        return new OrgResponseDTO(
                "Organization successfully registered under tenant: ",
                savedOrg.getOrgCode(),
                savedUser.getUserCode(),
                initialPassword);
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
     * A private helper method to safely map an Organisation entity to its DTO
     * representation.
     * This explicitly calls the getters, which resolves the lazy-loaded proxies.
     */
    private OrganisationListDTO toOrganisationListDTO(Organisation org) {
        OrganisationListDTO dto = new OrganisationListDTO();

        dto.setOrgId(org.getOrgId());
        dto.setOrgCode(org.getOrgCode());
        dto.setOrgName(org.getOrgName());
        dto.setBrandName(org.getBrandName());
        dto.setOrgUrl(org.getOrgUrl());
        dto.setOrgType(org.getType() != null ? org.getType().name() : null);
        dto.setCity(org.getCity());
        dto.setState(org.getState());
        dto.setEstablishDate(org.getEstablishmentDate());
        dto.setGroup(org.getIsGroup());
        dto.setIsActive(org.getIsActive());
        dto.setTenantId(org.getTenantSchema());

        // --- THIS IS THE FIX ---
        // Safely access lazy-loaded fields to get the real data.
        if (org.getParentOrganisation() != null) {
            dto.setParentOrgId(org.getParentOrganisation().getOrgId());
        }

        if (org.getUser() != null) {
            dto.setOwnerName(org.getUser().getFirstName()); // Or getFullName() if you have it
            dto.setOwnerEmail(org.getUser().getEmail());
            dto.setOwnerMobile(String.valueOf(org.getUser().getMobileNumber()));

            // Fetch owner code from OwnerDetails
            ownerDetailsRepository.findByOrganization(org).ifPresent(owner -> {
                dto.setOwnerCode(owner.getOwnerCode());
            });
        }

        return dto;
    }

    /**
     * Updates an existing organization and its associated owner details.
     * This operation is transactional, ensuring data consistency across related
     * tables.
     *
     * @param orgId The ID of the organisation to update.
     * @param dto   The data transfer object containing the updated information.
     * @return A DTO confirming the successful update.
     */
    @Transactional
    public OrgResponseDTO updateOrganization(Long orgId, OrgUpdateDTO dto) {
        // --- Step 1: Fetch the core Organisation entity ---
        Organisation org = organizationRepository.findById(orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Organisation not found with ID: " + orgId));

        // --- Step 2: Map and update the Organisation entity's fields ---
        org.setOrgName(dto.orgName());
        org.setBrandName(dto.brandName());
        org.setOrgUrl(dto.orgUrl());
        org.setCity(dto.city());
        org.setState(dto.state());
        org.setEstablishmentDate(dto.establishmentDate());
        org.setIsGroup(dto.isGroup());
        org.setType(dto.orgType() != null ? OrganizationType.valueOf(dto.orgType()) : null);
        // Note: JPA Auditing should handle 'last_modified_by' and 'last_modified_date'
        // automatically.

        // --- Step 3: Fetch and update the related OwnerDetails and User entities ---
        // Find the owner details linked to this organization.
        OwnerDetails owner = ownerDetailsRepository.findByOrganization(org)
                .orElseThrow(
                        () -> new ResourceNotFoundException("OwnerDetails not found for Organisation ID: " + orgId));

        // Update the denormalized fields on OwnerDetails as per the ERD.
        owner.setOwnerName(dto.ownerName());
        owner.setOwnerEmail(dto.ownerEmail());
        owner.setOwnerMobile(dto.ownerMobile());

        // Now, update the single source of truth: the User entity.
        User user = owner.getUser();
        if (user == null) {
            throw new IllegalStateException(
                    "Data integrity issue: OwnerDetails with ID " + owner.getOwnerId() + " has no associated User.");
        }

        // Update the master user record.
        // A simple utility can be used to split the full name into first and last
        // names.
        updateUserFullName(user, dto.ownerName());
        user.setEmail(dto.ownerEmail());
        if (dto.ownerMobile() != null && !dto.ownerMobile().trim().isEmpty()) {
            user.setMobileNumber(Long.valueOf(dto.ownerMobile().trim()));
        } else {
            user.setMobileNumber(null);
        }

        // --- Step 4: Save all changes ---
        // Within a @Transactional method, JPA's dirty checking often makes explicit
        // save calls redundant
        // for managed entities. However, calling save() is explicit and clear.
        organizationRepository.save(org);
        ownerDetailsRepository.save(owner);
        userRepository.save(user);

        // --- Step 5: Return a success response ---
        return new OrgResponseDTO(
                "Organization successfully updated.",
                org.getOrgCode(),
                user.getUserCode(),
                null); // No password change on update
    }

    /**
     * A helper utility to split a full name and update the user object.
     */
    private void updateUserFullName(User user, String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return;
        }
        String[] names = fullName.trim().split("\\s+", 2);
        user.setFirstName(names[0]);
        user.setLastName(names.length > 1 ? names[1] : ""); // Handle cases with no last name
    }

    /**
     * Retrieves a list of all active organizations that are marked as groups.
     *
     * @return A list of ParentOrgDTOs suitable for a dropdown.
     */
    public List<ParentOrgDTO> getParentOrganizations() {
        // 1. Fetch all parent-eligible organizations from the database.
        List<Organisation> groupOrganisations = organizationRepository.findByIsGroupTrueAndIsActiveTrue();

        // 2. Map the full Organisation entities to the lightweight ParentOrgDTO.
        return groupOrganisations.stream()
                .map(org -> new ParentOrgDTO(org.getOrgId(), org.getOrgName())) // Assuming getId() and getName()
                                                                                // methods
                .collect(Collectors.toList());
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
     * Creates and saves the OwnerDetails entity to link the User and Organisation.
     */
    private void createOwnerDetails(OrgRequestDTO request, User user, Organisation org) {
        logger.info("👑 [Owner] Creating owner details for user {} and org {}", user.getUserName(), org.getOrgName());
        OwnerDetails owner = new OwnerDetails();
        owner.setOwnerCode("OWNR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        owner.setOwnerName(request.getOwnerName());
        owner.setOwnerEmail(request.getOwnerEmail());
        owner.setOwnerMobile(request.getOwnerMobile());
        // owner.setGender(request.getGender());
        owner.setUser(user);
        owner.setOrganization(org);
        ownerDetailsRepository.save(owner);
    }

    /** Generates a unique username securely to avoid collisions. */
    private String generateUniqueUserName(String fullName) {
        String baseName = fullName != null ? fullName.toLowerCase().replaceAll("[^a-z0-9]", "_") : "user";
        String randomSuffix = UUID.randomUUID().toString().substring(0, 6);
        return baseName + "_" + randomSuffix;
    }

    /** Generates a cryptographically secure random password meeting complexity requirements. */
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder(12);
        
        for (int i = 0; i < 12; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        
        // Guarantee complexity requirements
        sb.setCharAt(0, "ABCDEFGHIJKLMNOPQRSTUVWXYZ".charAt(random.nextInt(26)));
        sb.setCharAt(1, "abcdefghijklmnopqrstuvwxyz".charAt(random.nextInt(26)));
        sb.setCharAt(2, "0123456789".charAt(random.nextInt(10)));
        sb.setCharAt(3, "!@#$%^&*".charAt(random.nextInt(8)));
        
        // Shuffle the characters
        List<Character> charList = sb.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
        java.util.Collections.shuffle(charList, random);
        
        return charList.stream().collect(StringBuilder::new, StringBuilder::append, StringBuilder::append).toString();
    }

    /**
     * Helper method to create and save a new Organisation.
     * It now includes the logic to link to a parent organization.
     */
    private Organisation createOrganisation(OrgRequestDTO request, User ownerUser) {
        logger.info("🏢 [Organisation] Creating new organisation: {}", request.getOrgName());
        Organisation newOrg = new Organisation();

        // --- NEW LOGIC for Parent Organization Mapping ---
        if (!request.getIsAGroup() && request.getParentOrgId() != null) {
            // If the new org is not a group and a parent ID is provided, find the parent.
            Organisation parentOrg = organizationRepository.findById(request.getParentOrgId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Parent Organization not found with ID: " + request.getParentOrgId()));

            // Set the parent organization on the new (child) organization.
            newOrg.setParentOrganisation(parentOrg);
        }
        // ---------------------------------------------------
        newOrg.setOrgCode("ORG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        newOrg.setOrgName(request.getOrgName());
        newOrg.setBrandName(request.getBrandName());
        newOrg.setOrgUrl(request.getOrgUrl());
        newOrg.setCity(request.getCity());
        newOrg.setState(request.getState());
        newOrg.setEstablishmentDate(request.getEstablishDate());
        newOrg.setIsGroup(request.getIsAGroup());
        newOrg.setType(request.getOrgType() != null ? OrganizationType.valueOf(request.getOrgType()) : null);
        newOrg.setSubscriptionType(request.getSubscriptionType());
        newOrg.setIsActive(true); // Default to active on creation

        // Link to tenant schema if provided (set during onboarding)
        if (request.getTenantSchema() != null && !request.getTenantSchema().isEmpty()) {
            newOrg.setTenantSchema(request.getTenantSchema());
        }

        // Associate the owner User with this Organisation (as per your ER diagram)
        newOrg.setUser(ownerUser);

        return organizationRepository.save(newOrg);
    }

}
