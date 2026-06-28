package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.OrganizationSubscription;
import com.thinkerscave.platform.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationSubscriptionRepository extends JpaRepository<OrganizationSubscription, Long> {

    Optional<OrganizationSubscription> findByOrganization_Id(Long organizationId);

    Optional<OrganizationSubscription> findByOrganization_IdAndActiveTrue(Long organizationId);

    @Query("""
            SELECT s FROM OrganizationSubscription s
            WHERE s.active = true
            AND (:status IS NULL OR s.status = :status)
            AND (:search IS NULL OR LOWER(s.organization.organizationName) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<OrganizationSubscription> searchSubscriptions(
            @Param("status") SubscriptionStatus status,
            @Param("search") String search,
            Pageable pageable);

    @Query("""
            SELECT s FROM OrganizationSubscription s
            WHERE s.active = true
            AND s.status = com.thinkerscave.platform.enums.SubscriptionStatus.ACTIVE
            AND s.endDate BETWEEN :from AND :to
            """)
    List<OrganizationSubscription> findRenewalsDue(@Param("from") LocalDate from, @Param("to") LocalDate to);

    long countByStatus(SubscriptionStatus status);
}
