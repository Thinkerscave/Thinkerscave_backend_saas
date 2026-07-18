package com.thinkerscave.platform.service;

import com.thinkerscave.platform.dto.request.CustomerContactRequest;
import com.thinkerscave.platform.dto.request.CustomerRequest;
import com.thinkerscave.platform.dto.response.CustomerContactResponse;
import com.thinkerscave.platform.dto.response.CustomerDashboardResponse;
import com.thinkerscave.platform.dto.response.CustomerDetailResponse;
import com.thinkerscave.platform.dto.response.CustomerListItemResponse;
import com.thinkerscave.platform.dto.response.CustomerMetadataResponse;
import com.thinkerscave.platform.dto.response.CustomerResponse;
import com.thinkerscave.platform.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomerService {

    Page<CustomerListItemResponse> getCustomers(
            CustomerStatus status,
            String search,
            boolean activeOnly,
            String createdPreset,
            Pageable pageable);
    CustomerDashboardResponse getCustomerDashboard();

    CustomerMetadataResponse getCustomerMetadata();

    CustomerDetailResponse getCustomerById(Long id);

    CustomerResponse createCustomer(CustomerRequest request);

    CustomerResponse updateCustomer(Long id, CustomerRequest request);

    CustomerResponse updateCustomerStatus(Long id, CustomerStatus status);

    void archiveCustomer(Long id);

    void restoreCustomer(Long id);

    void permanentlyDeleteCustomer(Long id);

    List<CustomerContactResponse> getContacts(Long customerId);

    CustomerContactResponse addContact(Long customerId, CustomerContactRequest request);

    CustomerContactResponse updateContact(Long contactId, CustomerContactRequest request);

    void deleteContact(Long contactId);
}
