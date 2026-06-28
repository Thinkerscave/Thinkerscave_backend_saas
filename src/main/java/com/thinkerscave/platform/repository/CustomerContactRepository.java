package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.CustomerContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerContactRepository extends JpaRepository<CustomerContact, Long> {

    List<CustomerContact> findByCustomer_IdAndActiveTrueOrderByPrimaryContactDesc();

    Optional<CustomerContact> findByContactCode(String contactCode);

    boolean existsByCustomer_IdAndPrimaryContactTrue(Long customerId);

    long countByCustomer_IdAndActiveTrue(Long customerId);
}
