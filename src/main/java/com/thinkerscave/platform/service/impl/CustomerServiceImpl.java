package com.thinkerscave.platform.service.impl;

import com.thinkerscave.access.dto.UserCreationContext;
import com.thinkerscave.access.entity.Role;
import com.thinkerscave.access.entity.User;
import com.thinkerscave.access.repository.RoleRepository;
import com.thinkerscave.access.service.UserService;
import com.thinkerscave.platform.dto.request.CustomerContactPayload;
import com.thinkerscave.platform.dto.request.CustomerContactRequest;
import com.thinkerscave.platform.dto.request.CustomerRequest;
import com.thinkerscave.platform.dto.response.CustomerContactResponse;
import com.thinkerscave.platform.dto.response.CustomerDashboardResponse;
import com.thinkerscave.platform.dto.response.CustomerDetailResponse;
import com.thinkerscave.platform.dto.response.CustomerListItemResponse;
import com.thinkerscave.platform.dto.response.CustomerMetadataResponse;
import com.thinkerscave.platform.dto.response.CustomerResponse;
import com.thinkerscave.platform.dto.response.EnumOptionResponse;
import com.thinkerscave.platform.dto.response.OrganizationSummaryResponse;
import com.thinkerscave.platform.entity.Customer;
import com.thinkerscave.platform.entity.CustomerContact;
import com.thinkerscave.platform.enums.ContactType;
import com.thinkerscave.platform.enums.CustomerStatus;
import com.thinkerscave.platform.repository.CustomerContactRepository;
import com.thinkerscave.platform.repository.CustomerRepository;
import com.thinkerscave.platform.repository.OrganizationRepository;
import com.thinkerscave.platform.repository.OrganizationSubscriptionRepository;
import com.thinkerscave.platform.service.CustomerService;
import com.thinkerscave.shared.enums.CodeType;
import com.thinkerscave.shared.exceptions.AlreadyExistsException;
import com.thinkerscave.shared.exceptions.BadRequestException;
import com.thinkerscave.shared.exceptions.ResourceNotFoundException;
import com.thinkerscave.shared.service.CodeGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private static final String ROLE_OWNER_CODE = "ROLE_OWNER";

    private final CustomerRepository customerRepository;
    private final CustomerContactRepository contactRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final CodeGeneratorService codeGeneratorService;
    private final UserService userService;
    private final RoleRepository roleRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerListItemResponse> getCustomers(
            CustomerStatus status,
            String search,
            boolean activeOnly,
            String createdPreset,
            Pageable pageable) {
        LocalDateTime[] range = resolveCreatedRange(createdPreset);
        return customerRepository.searchCustomers(
                        activeOnly,
                        status,
                        blankToNull(search),
                        range[0],
                        range[1],
                        pageable)
                .map(this::toListItem);
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDashboardResponse getCustomerDashboard() {
        LocalDate today = LocalDate.now();
        LocalDate in30Days = today.plusDays(30);
        BigDecimal annualRevenue = subscriptionRepository.sumActiveAnnualRevenue();

        return CustomerDashboardResponse.builder()
                .totalCustomers(customerRepository.countByActiveTrue())
                .activeCustomers(customerRepository.countByStatus(CustomerStatus.ACTIVE))
                .trialCustomers(0L)
                .suspendedCustomers(customerRepository.countByStatus(CustomerStatus.SUSPENDED))
                .archivedCustomers(customerRepository.countByStatus(CustomerStatus.ARCHIVED))
                .totalOrganizations(organizationRepository.countByActiveTrue())
                .annualRevenue(annualRevenue != null ? annualRevenue : BigDecimal.ZERO)
                .renewals30Days(subscriptionRepository.findRenewalsDue(today, in30Days).size())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerMetadataResponse getCustomerMetadata() {
        return CustomerMetadataResponse.builder()
                .statuses(statusOptions())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDetailResponse getCustomerById(Long id) {
        Customer customer = findById(id);
        List<CustomerContactResponse> contacts = activeContacts(customer);

        List<OrganizationSummaryResponse> organizations = customer.getOrganizations().stream()
                .filter(o -> Boolean.TRUE.equals(o.getActive()))
                .map(o -> OrganizationSummaryResponse.builder()
                        .id(o.getId())
                        .organizationCode(o.getOrganizationCode())
                        .organizationName(o.getOrganizationName())
                        .shortName(o.getShortName())
                        .institutionType(o.getInstitutionType())
                        .status(o.getStatus())
                        .email(o.getEmail())
                        .mobileNumber(o.getMobileNumber())
                        .city(o.getCity())
                        .state(o.getState())
                        .country(o.getCountry())
                        .logoUrl(o.getLogoUrl())
                        .onboardingCompleted(o.getOnboardingCompleted())
                        .active(o.getActive())
                        .tenantIdentifier(o.getTenantRegistry() != null ? o.getTenantRegistry().getTenantIdentifier() : null)
                        .createdOn(o.getCreatedOn())
                        .build())
                .collect(Collectors.toList());

        return CustomerDetailResponse.builder()
                .id(customer.getId())
                .customerCode(customer.getCustomerCode())
                .customerName(customer.getCustomerName())
                .businessEmail(customer.getBusinessEmail())
                .mobileNumber(customer.getMobileNumber())
                .alternateMobileNumber(customer.getAlternateMobileNumber())
                .notes(customer.getNotes())
                .status(customer.getStatus())
                .ownerUserId(customer.getOwnerUserId())
                .active(customer.getActive())
                .primaryContact(findContact(contacts, ContactType.PRIMARY))
                .secondaryContact(findContact(contacts, ContactType.SECONDARY))
                .contacts(contacts)
                .organizations(organizations)
                .createdOn(customer.getCreatedOn())
                .createdBy(customer.getCreatedBy())
                .updatedOn(customer.getUpdatedOn())
                .updatedBy(customer.getUpdatedBy())
                .build();
    }

    @Override
    @Transactional
    public CustomerResponse createCustomer(CustomerRequest request) {
        validateCustomerRequest(request, null);

        String email = normalizeEmail(request.getBusinessEmail());
        if (customerRepository.existsByBusinessEmail(email)) {
            throw new AlreadyExistsException("Customer email already exists: " + email);
        }

        String code = codeGeneratorService.generate(CodeType.CUSTOMER);
        Customer customer = Customer.builder()
                .customerCode(code)
                .customerName(request.getCustomerName().trim())
                .businessEmail(email)
                .mobileNumber(normalizePhone(request.getMobileNumber()))
                .alternateMobileNumber(blankToNull(normalizePhone(request.getAlternateMobileNumber())))
                .notes(blankToNull(request.getNotes()))
                .status(CustomerStatus.ACTIVE)
                .active(true)
                .build();

        CustomerContact primary = buildContact(request.getPrimaryContact(), ContactType.PRIMARY);
        customer.addContact(primary);

        if (hasSecondaryPayload(request.getSecondaryContact())) {
            customer.addContact(buildContact(request.getSecondaryContact(), ContactType.SECONDARY));
        }

        customer = customerRepository.save(customer);

        User owner = createOwnerUser(customer, primary);
        customer.setOwnerUserId(owner.getId());
        customer = customerRepository.save(customer);

        log.info("Customer created: {} ownerUserId={} username={}", code, owner.getId(), owner.getUsername());
        return toResponse(customer, 0);
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        validateCustomerRequest(request, id);

        Customer customer = findById(id);
        String email = normalizeEmail(request.getBusinessEmail());
        if (customerRepository.existsByBusinessEmailAndIdNot(email, id)) {
            throw new AlreadyExistsException("Customer email already exists: " + email);
        }

        customer.setCustomerName(request.getCustomerName().trim());
        customer.setBusinessEmail(email);
        customer.setMobileNumber(normalizePhone(request.getMobileNumber()));
        customer.setAlternateMobileNumber(blankToNull(normalizePhone(request.getAlternateMobileNumber())));
        customer.setNotes(blankToNull(request.getNotes()));

        upsertContact(customer, ContactType.PRIMARY, request.getPrimaryContact(), true);
        if (hasSecondaryPayload(request.getSecondaryContact())) {
            upsertContact(customer, ContactType.SECONDARY, request.getSecondaryContact(), true);
        } else {
            deactivateContact(customer, ContactType.SECONDARY);
        }

        customer = customerRepository.save(customer);
        return toResponse(customer, organizationRepository.countByCustomer_IdAndActiveTrue(id));
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomerStatus(Long id, CustomerStatus status) {
        Customer customer = findById(id);
        customer.setStatus(status);
        if (status == CustomerStatus.ARCHIVED) {
            customer.setActive(false);
        } else {
            customer.setActive(true);
        }
        return toResponse(customerRepository.save(customer), organizationRepository.countByCustomer_IdAndActiveTrue(id));
    }

    @Override
    @Transactional
    public void archiveCustomer(Long id) {
        updateCustomerStatus(id, CustomerStatus.ARCHIVED);
        log.info("Customer archived: {}", id);
    }

    @Override
    @Transactional
    public void restoreCustomer(Long id) {
        Customer customer = findById(id);
        customer.setActive(true);
        customer.setStatus(CustomerStatus.ACTIVE);
        customerRepository.save(customer);
        log.info("Customer restored: {}", customer.getCustomerCode());
    }

    @Override
    @Transactional
    public void permanentlyDeleteCustomer(Long id) {
        Customer customer = findById(id);
        if (Boolean.TRUE.equals(customer.getActive()) && customer.getStatus() != CustomerStatus.ARCHIVED) {
            throw new BadRequestException("Only archived customers can be permanently deleted");
        }
        long orgCount = organizationRepository.countByCustomer_IdAndActiveTrue(id);
        if (orgCount > 0) {
            throw new BadRequestException("Cannot delete customer with active organizations");
        }
        customerRepository.delete(customer);
        log.info("Customer permanently deleted: {}", customer.getCustomerCode());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerContactResponse> getContacts(Long customerId) {
        findById(customerId);
        return contactRepository.findByCustomer_IdAndActiveTrueOrderByContactTypeAsc(customerId)
                .stream()
                .map(this::toContactResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomerContactResponse addContact(Long customerId, CustomerContactRequest request) {
        Customer customer = findById(customerId);
        if (request.getContactType() == ContactType.PRIMARY
                && contactRepository.existsByCustomer_IdAndContactTypeAndActiveTrue(customerId, ContactType.PRIMARY)) {
            throw new BadRequestException("Customer already has a primary contact");
        }
        if (request.getContactType() == ContactType.SECONDARY
                && contactRepository.existsByCustomer_IdAndContactTypeAndActiveTrue(customerId, ContactType.SECONDARY)) {
            throw new BadRequestException("Customer already has a secondary contact");
        }

        CustomerContactPayload payload = new CustomerContactPayload();
        payload.setFullName(request.getFullName());
        payload.setEmail(request.getEmail());
        payload.setMobileNumber(request.getMobileNumber());
        payload.setDesignation(request.getDesignation());

        CustomerContact contact = buildContact(payload, request.getContactType());
        customer.addContact(contact);
        customerRepository.save(customer);
        return toContactResponse(contact);
    }

    @Override
    @Transactional
    public CustomerContactResponse updateContact(Long contactId, CustomerContactRequest request) {
        CustomerContact contact = findContactById(contactId);
        contact.setFullName(request.getFullName().trim());
        contact.setEmail(normalizeEmail(request.getEmail()));
        contact.setMobileNumber(normalizePhone(request.getMobileNumber()));
        contact.setDesignation(blankToNull(request.getDesignation()));
        if (request.getContactType() != null) {
            contact.setContactType(request.getContactType());
        }
        return toContactResponse(contactRepository.save(contact));
    }

    @Override
    @Transactional
    public void deleteContact(Long contactId) {
        CustomerContact contact = findContactById(contactId);
        if (contact.getContactType() == ContactType.PRIMARY) {
            throw new BadRequestException("Primary contact cannot be deleted");
        }
        contact.setActive(false);
        contactRepository.save(contact);
    }

    private void validateCustomerRequest(CustomerRequest request, Long customerId) {
        if (request.getPrimaryContact() == null || !hasText(request.getPrimaryContact().getFullName())) {
            throw new BadRequestException("Primary contact full name is required");
        }
        CustomerContactPayload primary = request.getPrimaryContact();
        if (trim(primary.getFullName()).length() < 3) {
            throw new BadRequestException("Primary contact name must be at least 3 characters");
        }
        if (!hasText(primary.getEmail())) {
            throw new BadRequestException("Primary contact email is required");
        }
        if (!hasText(primary.getMobileNumber())) {
            throw new BadRequestException("Primary contact mobile is required");
        }

        String businessMobile = normalizePhone(request.getMobileNumber());
        String alternate = blankToNull(normalizePhone(request.getAlternateMobileNumber()));
        if (alternate != null && alternate.equals(businessMobile)) {
            throw new BadRequestException("Alternate mobile cannot be identical to business mobile");
        }

        if (hasSecondaryPayload(request.getSecondaryContact())) {
            CustomerContactPayload secondary = request.getSecondaryContact();
            if (!hasText(secondary.getFullName()) || trim(secondary.getFullName()).length() < 3) {
                throw new BadRequestException("Secondary contact name is required when secondary contact is provided");
            }
            if (!hasText(secondary.getEmail())) {
                throw new BadRequestException("Secondary contact email is required when secondary contact is provided");
            }
            if (!hasText(secondary.getMobileNumber())) {
                throw new BadRequestException("Secondary contact mobile is required when secondary contact is provided");
            }
        }
    }

    private boolean hasSecondaryPayload(CustomerContactPayload payload) {
        if (payload == null) return false;
        return hasText(payload.getFullName())
                || hasText(payload.getEmail())
                || hasText(payload.getMobileNumber())
                || hasText(payload.getDesignation());
    }

    private CustomerContact buildContact(CustomerContactPayload payload, ContactType type) {
        return CustomerContact.builder()
                .contactCode(codeGeneratorService.generate(CodeType.CONTACT))
                .contactType(type)
                .fullName(payload.getFullName().trim())
                .email(normalizeEmail(payload.getEmail()))
                .mobileNumber(normalizePhone(payload.getMobileNumber()))
                .designation(blankToNull(payload.getDesignation()))
                .active(true)
                .build();
    }

    private void upsertContact(Customer customer, ContactType type, CustomerContactPayload payload, boolean required) {
        CustomerContact existing = customer.getContacts().stream()
                .filter(c -> c.getContactType() == type && Boolean.TRUE.equals(c.getActive()))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            if (required || hasSecondaryPayload(payload)) {
                customer.addContact(buildContact(payload, type));
            }
            return;
        }

        existing.setFullName(payload.getFullName().trim());
        existing.setEmail(normalizeEmail(payload.getEmail()));
        existing.setMobileNumber(normalizePhone(payload.getMobileNumber()));
        existing.setDesignation(blankToNull(payload.getDesignation()));
        existing.setActive(true);
    }

    private void deactivateContact(Customer customer, ContactType type) {
        customer.getContacts().stream()
                .filter(c -> c.getContactType() == type && Boolean.TRUE.equals(c.getActive()))
                .forEach(c -> c.setActive(false));
    }

    private User createOwnerUser(Customer customer, CustomerContact primary) {
        Role ownerRole = roleRepository.findByRoleCode(ROLE_OWNER_CODE)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + ROLE_OWNER_CODE));

        String[] nameParts = splitContactName(primary.getFullName());
        UserCreationContext context = new UserCreationContext(
                nameParts[0],
                null,
                nameParts[1],
                customer.getBusinessEmail(),
                customer.getMobileNumber(),
                null,
                null,
                null,
                primary.getDesignation() != null ? primary.getDesignation() : "Owner"
        );
        return userService.createUser(context, ownerRole);
    }

    private static String[] splitContactName(String fullName) {
        String trimmed = fullName == null ? "" : fullName.trim();
        if (trimmed.isEmpty()) {
            return new String[]{"Owner", ""};
        }
        int space = trimmed.indexOf(' ');
        if (space < 0) {
            return new String[]{trimmed, ""};
        }
        return new String[]{trimmed.substring(0, space), trimmed.substring(space + 1).trim()};
    }

    private Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private CustomerContact findContactById(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerContact not found: " + id));
    }

    private List<CustomerContactResponse> activeContacts(Customer customer) {
        return customer.getContacts().stream()
                .filter(c -> Boolean.TRUE.equals(c.getActive()))
                .sorted(Comparator.comparing(c -> c.getContactType() == ContactType.PRIMARY ? 0 : 1))
                .map(this::toContactResponse)
                .collect(Collectors.toList());
    }

    private CustomerListItemResponse toListItem(Customer c) {
        CustomerContact primary = resolvePrimaryContact(c.getId());

        String ownerName = primary != null ? primary.getFullName() : null;
        String ownerEmail = primary != null && hasText(primary.getEmail())
                ? primary.getEmail()
                : c.getBusinessEmail();

        LocalDateTime created = c.getCreatedOn() != null ? c.getCreatedOn() : c.getUpdatedOn();
        LocalDateTime activityAt = c.getUpdatedOn() != null ? c.getUpdatedOn() : created;

        return CustomerListItemResponse.builder()
                .id(c.getId())
                .customerCode(c.getCustomerCode())
                .customerName(c.getCustomerName())
                .logoUrl(null)
                .domain(extractDomain(c.getBusinessEmail()))
                .ownerName(ownerName)
                .ownerEmail(ownerEmail)
                .organizationCount(organizationRepository.countByCustomer_IdAndActiveTrue(c.getId()))
                .status(c.getStatus())
                .createdDate(created)
                .lastActivity(formatRelative(activityAt))
                .lastActivityAt(activityAt)
                .active(c.getActive())
                .build();
    }

    private CustomerContact resolvePrimaryContact(Long customerId) {
        List<CustomerContact> contacts = contactRepository.findByCustomer_IdOrderByContactTypeAsc(customerId);
        return contacts.stream()
                .filter(c -> c.getContactType() == ContactType.PRIMARY)
                .findFirst()
                .or(() -> contacts.stream().findFirst())
                .orElse(null);
    }

    private LocalDateTime[] resolveCreatedRange(String preset) {
        if (preset == null || preset.isBlank() || "all".equalsIgnoreCase(preset)) {
            return new LocalDateTime[]{null, null};
        }
        LocalDate today = LocalDate.now();
        return switch (preset.toLowerCase()) {
            case "today" -> new LocalDateTime[]{today.atStartOfDay(), today.plusDays(1).atStartOfDay()};
            case "7d", "last7days" -> new LocalDateTime[]{today.minusDays(6).atStartOfDay(), today.plusDays(1).atStartOfDay()};
            case "30d", "last30days" -> new LocalDateTime[]{today.minusDays(29).atStartOfDay(), today.plusDays(1).atStartOfDay()};
            case "90d", "last90days" -> new LocalDateTime[]{today.minusDays(89).atStartOfDay(), today.plusDays(1).atStartOfDay()};
            case "year", "thisyear" -> new LocalDateTime[]{
                    LocalDate.of(today.getYear(), 1, 1).atStartOfDay(),
                    today.plusDays(1).atStartOfDay()
            };
            default -> new LocalDateTime[]{null, null};
        };
    }

    private static String extractDomain(String email) {
        if (email == null || !email.contains("@")) return null;
        String domain = email.substring(email.indexOf('@') + 1).trim().toLowerCase();
        return domain.isEmpty() ? null : domain;
    }

    private static String formatRelative(LocalDateTime at) {
        if (at == null) return "—";
        LocalDateTime now = LocalDateTime.now();
        if (at.isAfter(now)) return "just now";

        Duration duration = Duration.between(at, now);
        long minutes = duration.toMinutes();
        if (minutes < 1) return "just now";
        if (minutes < 60) return minutes == 1 ? "1 minute ago" : minutes + " minutes ago";

        long hours = duration.toHours();
        if (hours < 24) return hours == 1 ? "1 hour ago" : hours + " hours ago";

        long days = duration.toDays();
        if (days == 1) return "Yesterday";
        if (days < 30) return days + " days ago";

        long months = days / 30;
        if (months < 12) return months == 1 ? "1 month ago" : months + " months ago";

        long years = days / 365;
        return years == 1 ? "1 year ago" : years + " years ago";
    }

    private CustomerResponse toResponse(Customer c, long orgCount) {
        List<CustomerContactResponse> contacts = activeContacts(c);
        return CustomerResponse.builder()
                .id(c.getId())
                .customerCode(c.getCustomerCode())
                .customerName(c.getCustomerName())
                .businessEmail(c.getBusinessEmail())
                .mobileNumber(c.getMobileNumber())
                .alternateMobileNumber(c.getAlternateMobileNumber())
                .notes(c.getNotes())
                .status(c.getStatus())
                .ownerUserId(c.getOwnerUserId())
                .active(c.getActive())
                .organizationCount(orgCount)
                .primaryContact(findContact(contacts, ContactType.PRIMARY))
                .secondaryContact(findContact(contacts, ContactType.SECONDARY))
                .contacts(contacts)
                .createdOn(c.getCreatedOn())
                .createdBy(c.getCreatedBy())
                .updatedOn(c.getUpdatedOn())
                .updatedBy(c.getUpdatedBy())
                .build();
    }

    private CustomerContactResponse findContact(List<CustomerContactResponse> contacts, ContactType type) {
        return contacts.stream().filter(c -> c.getContactType() == type).findFirst().orElse(null);
    }

    private CustomerContactResponse toContactResponse(CustomerContact cc) {
        return CustomerContactResponse.builder()
                .id(cc.getId())
                .contactCode(cc.getContactCode())
                .customerId(cc.getCustomer() != null ? cc.getCustomer().getId() : null)
                .contactType(cc.getContactType())
                .fullName(cc.getFullName())
                .email(cc.getEmail())
                .mobileNumber(cc.getMobileNumber())
                .designation(cc.getDesignation())
                .active(cc.getActive())
                .createdOn(cc.getCreatedOn())
                .createdBy(cc.getCreatedBy())
                .build();
    }

    private List<EnumOptionResponse> statusOptions() {
        return Arrays.stream(CustomerStatus.values())
                .filter(value -> value != CustomerStatus.LEAD && value != CustomerStatus.TRIAL)
                .map(value -> EnumOptionResponse.builder()
                        .code(value.name())
                        .label(formatEnumLabel(value.name()))
                        .build())
                .collect(Collectors.toList());
    }

    private String formatEnumLabel(String name) {
        return Arrays.stream(name.split("_"))
                .map(part -> part.charAt(0) + part.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }

    private static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return null;
        String trimmed = phone.trim();
        return trimmed.isEmpty() ? null : trimmed.replaceAll("\\s+", " ");
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
