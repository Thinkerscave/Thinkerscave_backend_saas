package com.thinkerscave.platform.service.impl;

import com.thinkerscave.platform.dto.request.CustomerContactRequest;
import com.thinkerscave.platform.dto.request.CustomerRequest;
import com.thinkerscave.platform.dto.response.CustomerContactResponse;
import com.thinkerscave.platform.dto.response.CustomerDashboardResponse;
import com.thinkerscave.platform.dto.response.CustomerDetailResponse;
import com.thinkerscave.platform.dto.response.CustomerMetadataResponse;
import com.thinkerscave.platform.dto.response.CustomerResponse;
import com.thinkerscave.platform.dto.response.EnumOptionResponse;
import com.thinkerscave.platform.dto.response.OrganizationSummaryResponse;
import com.thinkerscave.platform.entity.Customer;
import com.thinkerscave.platform.entity.CustomerContact;
import com.thinkerscave.platform.enums.CustomerStatus;
import com.thinkerscave.platform.enums.CustomerType;
import com.thinkerscave.platform.enums.PreferredCommunication;
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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerServiceImpl implements CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerContactRepository contactRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationSubscriptionRepository subscriptionRepository;
    private final CodeGeneratorService codeGeneratorService;

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomers(CustomerStatus status, CustomerType customerType, String search, boolean activeOnly, Pageable pageable) {
        return customerRepository.searchCustomers(activeOnly, status, customerType, search, pageable)
                .map(c -> toResponse(c, organizationRepository.countByCustomer_IdAndActiveTrue(c.getId())));
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
                .trialCustomers(customerRepository.countByStatus(CustomerStatus.TRIAL))
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
                .statuses(enumOptions(CustomerStatus.class))
                .customerTypes(enumOptions(CustomerType.class))
                .preferredCommunications(enumOptions(PreferredCommunication.class))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerDetailResponse getCustomerById(Long id) {
        Customer customer = findById(id);
        List<CustomerContactResponse> contacts = contactRepository
                .findByCustomer_IdAndActiveTrueOrderByPrimaryContactDesc(id)
                .stream()
                .map(this::toContactResponse)
                .collect(Collectors.toList());

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
                .legalName(customer.getLegalName())
                .displayName(customer.getDisplayName())
                .customerType(customer.getCustomerType())
                .status(customer.getStatus())
                .email(customer.getEmail())
                .mobileNumber(customer.getMobileNumber())
                .alternateMobileNumber(customer.getAlternateMobileNumber())
                .website(customer.getWebsite())
                .taxNumber(customer.getTaxNumber())
                .registrationNumber(customer.getRegistrationNumber())
                .addressLine1(customer.getAddressLine1())
                .addressLine2(customer.getAddressLine2())
                .city(customer.getCity())
                .state(customer.getState())
                .country(customer.getCountry())
                .postalCode(customer.getPostalCode())
                .logoUrl(customer.getLogoUrl())
                .preferredCommunication(customer.getPreferredCommunication())
                .onboardingCompleted(customer.getOnboardingCompleted())
                .active(customer.getActive())
                .remarks(customer.getRemarks())
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
        if (customerRepository.existsByEmail(request.getEmail())) {
            throw new AlreadyExistsException("Customer email already exists: " + request.getEmail());
        }
        String code = codeGeneratorService.generate(CodeType.CUSTOMER);
        Customer customer = Customer.builder()
                .customerCode(code)
                .legalName(request.getLegalName())
                .displayName(request.getDisplayName())
                .customerType(request.getCustomerType())
                .status(request.getStatus() != null ? request.getStatus() : CustomerStatus.LEAD)
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .alternateMobileNumber(request.getAlternateMobileNumber())
                .website(request.getWebsite())
                .taxNumber(request.getTaxNumber())
                .registrationNumber(request.getRegistrationNumber())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .postalCode(request.getPostalCode())
                .logoUrl(request.getLogoUrl())
                .preferredCommunication(request.getPreferredCommunication())
                .onboardingCompleted(false)
                .active(true)
                .remarks(request.getRemarks())
                .build();
        customer = customerRepository.save(customer);
        log.info("Customer created: {}", code);
        return toResponse(customer, organizationRepository.countByCustomer_IdAndActiveTrue(customer.getId()));
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerRequest request) {
        Customer customer = findById(id);
        if (customerRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
            throw new AlreadyExistsException("Customer email already exists: " + request.getEmail());
        }
        customer.setLegalName(request.getLegalName());
        customer.setDisplayName(request.getDisplayName());
        customer.setCustomerType(request.getCustomerType());
        if (request.getStatus() != null) customer.setStatus(request.getStatus());
        customer.setEmail(request.getEmail());
        customer.setMobileNumber(request.getMobileNumber());
        customer.setAlternateMobileNumber(request.getAlternateMobileNumber());
        customer.setWebsite(request.getWebsite());
        customer.setTaxNumber(request.getTaxNumber());
        customer.setRegistrationNumber(request.getRegistrationNumber());
        customer.setAddressLine1(request.getAddressLine1());
        customer.setAddressLine2(request.getAddressLine2());
        customer.setCity(request.getCity());
        customer.setState(request.getState());
        customer.setCountry(request.getCountry());
        customer.setPostalCode(request.getPostalCode());
        customer.setLogoUrl(request.getLogoUrl());
        customer.setPreferredCommunication(request.getPreferredCommunication());
        customer.setRemarks(request.getRemarks());
        return toResponse(customerRepository.save(customer), organizationRepository.countByCustomer_IdAndActiveTrue(id));
    }

    @Override
    @Transactional
    public CustomerResponse updateCustomerStatus(Long id, CustomerStatus status) {
        Customer customer = findById(id);
        customer.setStatus(status);
        if (status == CustomerStatus.ARCHIVED) {
            customer.setActive(false);
        } else if (status != CustomerStatus.ARCHIVED) {
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

    // ── Contacts ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<CustomerContactResponse> getContacts(Long customerId) {
        findById(customerId); // validate customer exists
        return contactRepository.findByCustomer_IdAndActiveTrueOrderByPrimaryContactDesc(customerId)
                .stream()
                .map(this::toContactResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CustomerContactResponse addContact(Long customerId, CustomerContactRequest request) {
        Customer customer = findById(customerId);
        String code = codeGeneratorService.generate(CodeType.CONTACT);
        CustomerContact contact = CustomerContact.builder()
                .contactCode(code)
                .customer(customer)
                .fullName(request.getFullName())
                .designation(request.getDesignation())
                .contactType(request.getContactType())
                .email(request.getEmail())
                .mobileNumber(request.getMobileNumber())
                .alternateMobileNumber(request.getAlternateMobileNumber())
                .officePhone(request.getOfficePhone())
                .department(request.getDepartment())
                .primaryContact(Boolean.TRUE.equals(request.getPrimaryContact()))
                .billingContact(Boolean.TRUE.equals(request.getBillingContact()))
                .technicalContact(Boolean.TRUE.equals(request.getTechnicalContact()))
                .salesContact(Boolean.TRUE.equals(request.getSalesContact()))
                .supportContact(Boolean.TRUE.equals(request.getSupportContact()))
                .active(true)
                .remarks(request.getRemarks())
                .build();
        return toContactResponse(contactRepository.save(contact));
    }

    @Override
    @Transactional
    public CustomerContactResponse updateContact(Long contactId, CustomerContactRequest request) {
        CustomerContact contact = findContactById(contactId);
        contact.setFullName(request.getFullName());
        contact.setDesignation(request.getDesignation());
        contact.setContactType(request.getContactType());
        contact.setEmail(request.getEmail());
        contact.setMobileNumber(request.getMobileNumber());
        contact.setAlternateMobileNumber(request.getAlternateMobileNumber());
        contact.setOfficePhone(request.getOfficePhone());
        contact.setDepartment(request.getDepartment());
        if (request.getPrimaryContact() != null) contact.setPrimaryContact(request.getPrimaryContact());
        if (request.getBillingContact() != null) contact.setBillingContact(request.getBillingContact());
        if (request.getTechnicalContact() != null) contact.setTechnicalContact(request.getTechnicalContact());
        if (request.getSalesContact() != null) contact.setSalesContact(request.getSalesContact());
        if (request.getSupportContact() != null) contact.setSupportContact(request.getSupportContact());
        contact.setRemarks(request.getRemarks());
        return toContactResponse(contactRepository.save(contact));
    }

    @Override
    @Transactional
    public void deleteContact(Long contactId) {
        CustomerContact contact = findContactById(contactId);
        contact.setActive(false);
        contactRepository.save(contact);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Customer findById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer not found: " + id));
    }

    private CustomerContact findContactById(Long id) {
        return contactRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CustomerContact not found: " + id));
    }

    private CustomerResponse toResponse(Customer c, long orgCount) {
        return CustomerResponse.builder()
                .id(c.getId())
                .customerCode(c.getCustomerCode())
                .legalName(c.getLegalName())
                .displayName(c.getDisplayName())
                .customerType(c.getCustomerType())
                .status(c.getStatus())
                .email(c.getEmail())
                .mobileNumber(c.getMobileNumber())
                .alternateMobileNumber(c.getAlternateMobileNumber())
                .website(c.getWebsite())
                .taxNumber(c.getTaxNumber())
                .registrationNumber(c.getRegistrationNumber())
                .addressLine1(c.getAddressLine1())
                .addressLine2(c.getAddressLine2())
                .city(c.getCity())
                .state(c.getState())
                .country(c.getCountry())
                .postalCode(c.getPostalCode())
                .logoUrl(c.getLogoUrl())
                .preferredCommunication(c.getPreferredCommunication())
                .onboardingCompleted(c.getOnboardingCompleted())
                .active(c.getActive())
                .remarks(c.getRemarks())
                .organizationCount(orgCount)
                .createdOn(c.getCreatedOn())
                .createdBy(c.getCreatedBy())
                .updatedOn(c.getUpdatedOn())
                .updatedBy(c.getUpdatedBy())
                .build();
    }

    private CustomerContactResponse toContactResponse(CustomerContact cc) {
        return CustomerContactResponse.builder()
                .id(cc.getId())
                .contactCode(cc.getContactCode())
                .customerId(cc.getCustomer().getId())
                .fullName(cc.getFullName())
                .designation(cc.getDesignation())
                .contactType(cc.getContactType())
                .email(cc.getEmail())
                .mobileNumber(cc.getMobileNumber())
                .alternateMobileNumber(cc.getAlternateMobileNumber())
                .officePhone(cc.getOfficePhone())
                .department(cc.getDepartment())
                .primaryContact(cc.getPrimaryContact())
                .billingContact(cc.getBillingContact())
                .technicalContact(cc.getTechnicalContact())
                .salesContact(cc.getSalesContact())
                .supportContact(cc.getSupportContact())
                .active(cc.getActive())
                .remarks(cc.getRemarks())
                .createdOn(cc.getCreatedOn())
                .createdBy(cc.getCreatedBy())
                .build();
    }

    private <E extends Enum<E>> List<EnumOptionResponse> enumOptions(Class<E> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(value -> EnumOptionResponse.builder()
                        .code(value.name())
                        .label(formatEnumLabel(value.name()))
                        .build())
                .collect(Collectors.toList());
    }

    private String formatEnumLabel(String code) {
        return Arrays.stream(code.split("_"))
                .map(part -> part.charAt(0) + part.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}
