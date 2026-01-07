package com.thinkerscave.common.orgm.service.serviceImp;

import com.thinkerscave.common.exception.ResourceNotFoundException;
import com.thinkerscave.common.multitenancy.TenantContext;
import com.thinkerscave.common.orgm.domain.Organisation;
import com.thinkerscave.common.orgm.domain.OwnerDetails;
import com.thinkerscave.common.orgm.dto.*;
import com.thinkerscave.common.orgm.repository.OrganizationRepository;
import com.thinkerscave.common.orgm.repository.OwnerDetailsRepository;
import com.thinkerscave.common.orgm.service.OrganizationService;
import com.thinkerscave.common.usrm.domain.User;
import com.thinkerscave.common.usrm.mapper.UserMapper;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());


    /**
     * Saves organization along with user and owner details.
     */
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
    public OrgResponseDTO saveOrganization(OrgRequestDTO request, String schema) {
//        String schema = TenantContext.getTenant();
//        if (schema == null) {
//            throw new IllegalStateException("Tenant (schema) not set. Please provide 'X-Tenant-ID' in the header.");
//        }

        // Step 1: Find an existing user or create a new one.
        User savedUser = findOrCreateUser(request, schema);

        // Step 2: Create the new Organisation, handling the parent relationship.
        Organisation savedOrg = createOrganisation(request, savedUser);

        // Step 3: Create the OwnerDetails to link the User and Organisation.
        createOwnerDetails(request, savedUser, savedOrg);

        // TODO: Consider sending a welcome email to the user with their generated username
        // and instructions to use the "Forgot Password" feature to set their password.

        return new OrgResponseDTO(
                "Organization successfully registered under tenant: ",
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
        dto.setOrgCode(org.getOrgCode());
        dto.setOrgName(org.getOrgName());
        dto.setBrandName(org.getBrandName());
        dto.setOrgUrl(org.getOrgUrl());
        dto.setOrgType(org.getType());
        dto.setCity(org.getCity());
        dto.setState(org.getState());
        dto.setEstablishDate(org.getEstablishmentDate());
        dto.setGroup(org.getIsGroup());
        dto.setIsActive(org.getIsActive());

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

    /**
     * Updates an existing organization and its associated owner details.
     * This operation is transactional, ensuring data consistency across related tables.
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
        org.setType(dto.orgType());
        // Note: JPA Auditing should handle 'last_modified_by' and 'last_modified_date' automatically.

        // --- Step 3: Fetch and update the related OwnerDetails and User entities ---
        // Find the owner details linked to this organization.
        OwnerDetails owner = ownerDetailsRepository.findByOrganization(org)
                .orElseThrow(() -> new ResourceNotFoundException("OwnerDetails not found for Organisation ID: " + orgId));

        // Update the denormalized fields on OwnerDetails as per the ERD.
        owner.setOwnerName(dto.ownerName());
        owner.setOwnerEmail(dto.ownerEmail());
        owner.setOwnerMobile(dto.ownerMobile());

        // Now, update the single source of truth: the User entity.
        User user = owner.getUser();
        if (user == null) {
            throw new IllegalStateException("Data integrity issue: OwnerDetails with ID " + owner.getOwnerId() + " has no associated User.");
        }

        // Update the master user record.
        // A simple utility can be used to split the full name into first and last names.
        updateUserFullName(user, dto.ownerName());
        user.setEmail(dto.ownerEmail());
        user.setMobileNumber(Long.parseLong(dto.ownerMobile())); // Assuming mobile is stored as a long

        // --- Step 4: Save all changes ---
        // Within a @Transactional method, JPA's dirty checking often makes explicit save calls redundant
        // for managed entities. However, calling save() is explicit and clear.
        organizationRepository.save(org);
        ownerDetailsRepository.save(owner);
        userRepository.save(user);

        // --- Step 5: Return a success response ---
        return new OrgResponseDTO(
                "Organization successfully updated.",
                org.getOrgCode(),
                user.getUserCode()
        );
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
                .map(org -> new ParentOrgDTO(org.getOrgId(), org.getOrgName())) // Assuming getId() and getName() methods
                .collect(Collectors.toList());
    }

    /**
     * Soft-deletes an organization by marking it inactive.
     */
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
     *//*
    private User findOrCreateUser(OrgRequestDTO request, String schema) {
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
                    //String rawPassword = generateRandomPassword();
                    String rawPassword = "password";
                    newUser.setPassword(passwordEncoder.encode(rawPassword));

                    newUser.setUserCode("USER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                    // Set default roles, active status, etc. here if needed

                    // Save in TENANT schema asynchronously
                    CompletableFuture
                            .supplyAsync(() -> {
                                try {
                                    TenantContext.setCurrentTenant(schema);
                                    User user = new User();
                                    user.setFirstName(request.getOwnerName()); // Assume ownerName is the full name for simplicity
                                    user.setLastName(""); // Can be enhanced to split name
                                    user.setEmail(request.getOwnerEmail());
                                    user.setMobileNumber(Long.valueOf(request.getOwnerMobile()));
                                    user.setUserName(generateUniqueUserName(request.getOwnerName()));

                                    // --- SECURITY FIX: Hash the password before saving ---
                                    //String rawPassword = generateRandomPassword();
                                    String defaultRawPassword = "password";
                                    user.setPassword(passwordEncoder.encode(defaultRawPassword));

                                    user.setSchemaName(schema);
                                    user.setUserCode("USER-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
                                    return userRepository.save(user);
                                } finally {
                                    TenantContext.clear();
                                }
                            }, executorService)
                            .exceptionally(ex -> {
                                logger.error(
                                        "❌ Failed to save user in tenant schema [{}]",
                                        ex);
                                return null;
                            });
                    // schema information only save in Public schema for each user
                    newUser.setSchemaName(schema);
                    return userRepository.save(newUser);
                });
    }*/

    /**
     * Finds an existing user by email or creates a new user
     * in both PUBLIC and TENANT schemas.
     */
    private User findOrCreateUser(OrgRequestDTO request, String schema) {

        logger.info("🔍 [User-Service] Start findOrCreateUser | email={} | schema={}",
                request.getOwnerEmail(), schema);

        // Step 1: Validate request data
        if (request.getOwnerEmail() == null || request.getOwnerEmail().isEmpty()) {
            logger.error("❌ [Validation] Owner email is missing");
            throw new IllegalArgumentException("Owner email must not be null or empty");
        }

        // Step 2: Try to find user in PUBLIC schema
        return userRepository.findByEmail(request.getOwnerEmail())
                .map(existingUser -> {
                    logger.info("✅ [User-Service] User already exists | userId={}",
                            existingUser.getId());
                    return existingUser;
                })
                .orElseGet(() -> {
                    logger.warn("⚠️ [User-Service] User not found. Creating new user | email={}",
                            request.getOwnerEmail());
                    return createUserForPublicAndTenant(request, schema);
                });
    }

    /**
     * Creates a user in PUBLIC schema synchronously
     * and TENANT schema asynchronously.
     */
    private User createUserForPublicAndTenant(OrgRequestDTO request, String schema) {

        logger.info("🚀 [User-Service] User creation started | schema={}", schema);

        // Step 3: Generate shared credentials
        String username = generateUniqueUserName(request.getOwnerName());
        String rawPassword = generateRandomPassword(); // Recommended
        logger.info("🔐 [User-Service] Credentials generated |------- rawPassword={}", rawPassword);
        String encodedPassword = passwordEncoder.encode("admin@123");

        logger.info("🔐 [User-Service] Credentials generated | username={}", username);

        // Step 4: Build user object for PUBLIC schema
        User publicUser = UserMapper.buildUser(request, username, encodedPassword, schema);

        // Step 5: Save user in PUBLIC schema
        User savedPublicUser;
        try {
            savedPublicUser = userRepository.save(publicUser);
            logger.info("✅ [PUBLIC-SCHEMA] User saved successfully | userId={}",
                    savedPublicUser.getId());
        } catch (Exception ex) {
            logger.error("❌ [PUBLIC-SCHEMA] Failed to save user", ex);
            throw ex; // Critical failure → stop flow
        }

        // Step 6: Save user in TENANT schema asynchronously
        saveUserInTenantSchemaAsync(request, username, encodedPassword, schema);

        logger.info("🏁 [User-Service] User creation flow completed (tenant async started)");

        return savedPublicUser;
    }

    /**
     * Saves user in tenant schema asynchronously.
     * Failure here should NOT affect PUBLIC schema save.
     */
    private void saveUserInTenantSchemaAsync(
            OrgRequestDTO request,
            String username,
            String encodedPassword,
            String schema) {

        logger.info("⏳ [TENANT-SCHEMA] Async user save initiated | schema={}", schema);

        CompletableFuture.runAsync(() -> {
                    try {
                        // Step 7: Set tenant context
                        TenantContext.setCurrentTenant(schema);
                        logger.info("🔁 [TENANT-CONTEXT] Tenant context set | schema={}", schema);

                        // Step 8: Build tenant user
                        User tenantUser = UserMapper.buildUser(request, username, encodedPassword, schema);

                        // Step 9: Save user in tenant schema
                        userRepository.save(tenantUser);
                        logger.info("✅ [TENANT-SCHEMA] User saved successfully | email={}",
                                tenantUser.getEmail());

                    } catch (Exception ex) {
                        // Tenant failure should be logged but not crash system
                        logger.error("❌ [TENANT-SCHEMA] Failed to save user | schema={}", schema, ex);

                    } finally {
                        // Step 10: Clear tenant context
                        TenantContext.clear();
                        logger.info("🧹 [TENANT-CONTEXT] Tenant context cleared | schema={}", schema);
                    }
                }, executorService)
                .exceptionally(ex -> {
                    logger.error("❌ [ASYNC-EXECUTOR] Unexpected async failure | schema={}", schema, ex);
                    return null;
                });
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

    /**
     * Generates a unique username using name and timestamp.
     */
    private String generateUniqueUserName(String fullName) {
        return fullName.toLowerCase().replace(" ", "_") + "_" + System.currentTimeMillis() % 10000;
    }

    /**
     * Generates a random 10-character password.
     */
    private String generateRandomPassword() {
        return UUID.randomUUID().toString().substring(0, 10);
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
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Organization not found with ID: " + request.getParentOrgId()));

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
        newOrg.setType(request.getOrgType());
        newOrg.setSubscriptionType(request.getSubscriptionType());
        newOrg.setIsActive(true); // Default to active on creation

        // Associate the owner User with this Organisation (as per your ER diagram)
        newOrg.setUser(ownerUser);

        return organizationRepository.save(newOrg);
    }

}

