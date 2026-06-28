package com.thinkerscave.platform.repository;

import com.thinkerscave.platform.entity.Promotion;
import com.thinkerscave.platform.enums.PromotionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    boolean existsByPromotionCode(String promotionCode);

    boolean existsByPromotionCodeAndIdNot(String promotionCode, Long id);

    Optional<Promotion> findByPromotionCode(String promotionCode);

    List<Promotion> findByActiveTrueOrderByCreatedOnDesc();

    List<Promotion> findByStatusAndActiveTrue(PromotionStatus status);
}
