package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.OrganizationPromotion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationPromotionRepository extends JpaRepository<OrganizationPromotion, Long> {

    List<OrganizationPromotion> findByOrganization_IdAndActiveTrueOrderByCreatedOnDesc(Long organizationId);

    boolean existsByOrganization_IdAndPromotion_Id(Long organizationId, Long promotionId);
}
