package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.request.CustomerContactRequest;
import com.thinkerscave.platform.dto.request.CustomerRequest;
import com.thinkerscave.platform.dto.response.CustomerContactResponse;
import com.thinkerscave.platform.dto.response.CustomerDetailResponse;
import com.thinkerscave.platform.dto.response.CustomerResponse;
import com.thinkerscave.platform.enums.CustomerStatus;
import com.thinkerscave.platform.enums.CustomerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerService {

    Page<CustomerResponse> getCustomers(CustomerStatus status, CustomerType customerType, String search, Pageable pageable);

    CustomerDetailResponse getCustomerById(Long id);

    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse updateCustomer(Long id, CustomerRequest request);

    void archiveCustomer(Long id);

    // Contacts
    List<CustomerContactResponse> getContacts(Long customerId);

    CustomerContactResponse addContact(Long customerId, CustomerContactRequest request);

    CustomerContactResponse updateContact(Long contactId, CustomerContactRequest request);

    void deleteContact(Long contactId);
}
