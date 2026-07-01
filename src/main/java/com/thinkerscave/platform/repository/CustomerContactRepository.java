package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.CustomerContact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerContactRepository extends JpaRepository<CustomerContact, Long> {

    @Query("""
            SELECT c FROM CustomerContact c
            WHERE c.customer.id = :customerId AND c.active = true
            ORDER BY c.primaryContact DESC
            """)
    List<CustomerContact> findByCustomer_IdAndActiveTrueOrderByPrimaryContactDesc(@Param("customerId") Long customerId);

    Optional<CustomerContact> findByContactCode(String contactCode);

    boolean existsByCustomer_IdAndPrimaryContactTrue(Long customerId);

    long countByCustomer_IdAndActiveTrue(Long customerId);
}
