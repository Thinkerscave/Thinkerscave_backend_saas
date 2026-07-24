package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.Customer;
import com.thinkerscave.platform.enums.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    boolean existsByBusinessEmail(String businessEmail);

    boolean existsByBusinessEmailAndIdNot(String businessEmail, Long id);

    boolean existsByCustomerCode(String customerCode);

    Optional<Customer> findByCustomerCode(String customerCode);

    Optional<Customer> findByBusinessEmail(String businessEmail);

    @Query("""
            SELECT c FROM Customer c
            WHERE (:activeOnly = false OR c.active = true)
            AND (:status IS NULL OR c.status = :status)
            AND (:hasCreatedFrom = false OR c.createdOn >= :createdFrom)
            AND (:hasCreatedTo = false OR c.createdOn < :createdTo)
            AND (
                :search IS NULL OR :search = ''
                OR LOWER(c.customerName) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.businessEmail) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.customerCode) LIKE LOWER(CONCAT('%', :search, '%'))
                OR LOWER(c.mobileNumber) LIKE LOWER(CONCAT('%', :search, '%'))
                OR EXISTS (
                    SELECT 1 FROM CustomerContact ct
                    WHERE ct.customer = c AND ct.active = true
                    AND (
                        LOWER(COALESCE(ct.fullName, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                        OR LOWER(COALESCE(ct.email, '')) LIKE LOWER(CONCAT('%', :search, '%'))
                    )
                )
            )
            """)
    Page<Customer> searchCustomers(
            @Param("activeOnly") boolean activeOnly,
            @Param("status") CustomerStatus status,
            @Param("search") String search,
            @Param("hasCreatedFrom") boolean hasCreatedFrom,
            @Param("createdFrom") LocalDateTime createdFrom,
            @Param("hasCreatedTo") boolean hasCreatedTo,
            @Param("createdTo") LocalDateTime createdTo,
            Pageable pageable);

    long countByActiveTrue();

    long countByStatus(CustomerStatus status);
}
