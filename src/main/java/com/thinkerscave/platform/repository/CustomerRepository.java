package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.Customer;
import com.thinkerscave.platform.enums.CustomerStatus;
import com.thinkerscave.platform.enums.CustomerType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByCustomerCode(String customerCode);

    Optional<Customer> findByCustomerCode(String customerCode);

    Optional<Customer> findByEmail(String email);

    @Query("""
            SELECT c FROM Customer c
            WHERE (:activeOnly = false OR c.active = true)
            AND (:status IS NULL OR c.status = :status)
            AND (:customerType IS NULL OR c.customerType = :customerType)
            AND (:search IS NULL OR :search = '' OR LOWER(c.displayName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.legalName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.email) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<Customer> searchCustomers(
            @Param("activeOnly") boolean activeOnly,
            @Param("status") CustomerStatus status,
            @Param("customerType") CustomerType customerType,
            @Param("search") String search,
            Pageable pageable);

    long countByActiveTrue();

    long countByStatus(CustomerStatus status);
}
