package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.CustomerContact;
import com.thinkerscave.platform.enums.ContactType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerContactRepository extends JpaRepository<CustomerContact, Long> {

    List<CustomerContact> findByCustomer_IdAndActiveTrueOrderByContactTypeAsc(Long customerId);

    List<CustomerContact> findByCustomer_IdOrderByContactTypeAsc(Long customerId);

    Optional<CustomerContact> findByCustomer_IdAndContactTypeAndActiveTrue(Long customerId, ContactType contactType);

    Optional<CustomerContact> findByContactCode(String contactCode);

    boolean existsByCustomer_IdAndContactTypeAndActiveTrue(Long customerId, ContactType contactType);

    long countByCustomer_IdAndActiveTrue(Long customerId);
}
